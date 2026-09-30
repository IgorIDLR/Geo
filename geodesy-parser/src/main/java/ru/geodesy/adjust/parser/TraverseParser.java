package ru.geodesy.adjust.parser;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.*;
import ru.geodesy.adjust.model.measurement.AzimuthMeasurement;
import ru.geodesy.adjust.model.measurement.DistanceMeasurement;
import ru.geodesy.adjust.model.measurement.HorizontalAngleMeasurement;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.common.AngleUtils;
import ru.geodesy.common.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбор упрощённого формата TRAVERSE — последовательности точек
 * с углами поворота и расстояниями до следующей.
 * <p>
 * Формат:
 * <pre>
 * {
 *   "type": "TRAVERSE",
 *   "initial": {
 *     "startPoint": "P1",
 *     "startToSecondAzimuth": "45°00′00″",
 *     "coordinates": [ ... ]
 *   },
 *   "legs": [
 *     { "pointId": "P3", "angle": "150°30′15″", "distance": 120.5 }
 *   ],
 *   "closure": { ... }
 * }
 * </pre>
 */
@Component
public class TraverseParser {

    private final PointParser pointParser;

    public TraverseParser(PointParser pointParser) {
        this.pointParser = pointParser;
    }

    public Network parse(JsonNode root) {
        JsonNode initialNode = root.get("initial");
        if (initialNode == null || !initialNode.isObject()) {
            throw new ParserException("TRAVERSE: отсутствует блок 'initial'");
        }

        // 1. Разбираем начальные точки
        List<Point> points = new ArrayList<>();
        JsonNode coordsNode = initialNode.get("coordinates");
        if (coordsNode == null || !coordsNode.isArray()) {
            throw new ParserException("TRAVERSE: 'initial.coordinates' обязателен");
        }
        points.addAll(pointParser.parse(coordsNode));

        String startPoint = Preconditions.requireNonBlank(
                PointParser.textOrNull(initialNode, "startPoint"),
                "traverse.startPoint");

        // 2. Проверяем наличие стартовой точки
        findPoint(points, startPoint);

        // 3. Разбираем сегменты (legs)
        JsonNode legsNode = root.get("legs");
        if (legsNode == null || !legsNode.isArray() || legsNode.isEmpty()) {
            throw new ParserException("TRAVERSE: 'legs' обязателен и не должен быть пустым");
        }

        List<Measurement> measurements = new ArrayList<>();
        List<String> pointSequence = new ArrayList<>();
        pointSequence.add(startPoint);

        // Ищем точку, следующую сразу за стартовой (второй пункт хода)
        String secondPoint = findSecondPoint(points, initialNode, startPoint);
        pointSequence.add(secondPoint);

        // 4. Начальный азимут (опционально)
        String startAz = PointParser.textOrNull(initialNode, "startToSecondAzimuth");
        if (startAz != null) {
            double azRad = parseAngle(startAz);
            ParameterSet tempParams = ParameterSet.fromPoints(points);
            measurements.add(new AzimuthMeasurement(azRad, 1e-8,
                    startPoint, secondPoint, tempParams));
        }

        // 5. Обрабатываем legs
        for (JsonNode leg : legsNode) {
            String newPointId = Preconditions.requireNonBlank(
                    PointParser.textOrNull(leg, "pointId"), "leg.pointId");
            double distance = PointParser.numberOrDefault(leg, "distance", 0);
            if (distance <= 0) {
                throw new ParserException("TRAVERSE: distance должна быть > 0");
            }

            // Создаём новую точку
            points.add(new Point(newPointId, false, null, null, null));

            // Создаём измерение угла и расстояния
            String angleText = PointParser.textOrNull(leg, "angle");
            if (angleText == null) {
                throw new ParserException("TRAVERSE: 'angle' обязателен в каждом leg");
            }
            double angleRad = parseAngle(angleText);

            ParameterSet params = ParameterSet.fromPoints(points);
            String prev = pointSequence.get(pointSequence.size() - 1);
            String prevPrev = pointSequence.size() >= 2
                    ? pointSequence.get(pointSequence.size() - 2) : null;

            if (prevPrev != null) {
                measurements.add(new HorizontalAngleMeasurement(
                        angleRad, 4e-8, prevPrev, prev, newPointId, params));
            }
            measurements.add(new DistanceMeasurement(
                    distance, 1e-5, prev, newPointId, params));

            pointSequence.add(newPointId);
        }

        // 6. Обработка closure
        JsonNode closureNode = root.get("closure");
        if (closureNode != null && closureNode.has("enabled")
                && closureNode.get("enabled").asBoolean(false)) {
            processClosure(closureNode, measurements, points, pointSequence);
        }

        NetworkType type = NetworkType.fixed();
        return new Network(points, measurements, type, 1.0, 2);
    }

    // ============================================================
    // Вспомогательные методы
    // ============================================================

    private String findSecondPoint(List<Point> points, JsonNode initialNode,
                                   String startPoint) {
        // Если в coordinates явно задана вторая точка — берём её
        for (Point p : points) {
            if (!p.id().equals(startPoint)) return p.id();
        }
        throw new ParserException(
                "TRAVERSE: не найдена вторая точка хода (кроме " + startPoint + ")");
    }

    private void processClosure(JsonNode closureNode, List<Measurement> measurements,
                                List<Point> points, List<String> pointSequence) {
        String finalPointId = Preconditions.requireNonBlank(
                PointParser.textOrNull(closureNode, "finalPointId"),
                "closure.finalPointId");
        double finalDistance = PointParser.numberOrDefault(closureNode, "finalDistance", 0);

        String last = pointSequence.get(pointSequence.size() - 1);
        String prev = pointSequence.get(pointSequence.size() - 2);

        ParameterSet params = ParameterSet.fromPoints(points);

        String finalAngleText = PointParser.textOrNull(closureNode, "finalAngle");
        if (finalAngleText != null) {
            double angleRad = parseAngle(finalAngleText);
            measurements.add(new HorizontalAngleMeasurement(
                    angleRad, 4e-8, prev, last, finalPointId, params));
        }
        if (finalDistance > 0) {
            measurements.add(new DistanceMeasurement(
                    finalDistance, 1e-5, last, finalPointId, params));
        }
    }

    private Point findPoint(List<Point> points, String id) {
        for (Point p : points) {
            if (p.id().equals(id)) return p;
        }
        throw new ParserException("TRAVERSE: точка '" + id + "' отсутствует");
    }

    /**
     * Разбирает угол из строки DMS, объекта {d,m,s} или числа.
     */
    private double parseAngle(String text) {
        return AngleUtils.parseDmsToRadians(text);
    }

    private double parseAngle(JsonNode node) {
        if (node.isNumber()) return node.asDouble();
        if (node.isObject()) {
            double d = PointParser.numberOrDefault(node, "d", 0);
            double m = PointParser.numberOrDefault(node, "m", 0);
            double s = PointParser.numberOrDefault(node, "s", 0);
            return AngleUtils.dmsToRadians(d, m, s);
        }
        if (node.isTextual()) return AngleUtils.parseDmsToRadians(node.asText());
        throw new ParserException("Неподдерживаемый формат угла");
    }
}