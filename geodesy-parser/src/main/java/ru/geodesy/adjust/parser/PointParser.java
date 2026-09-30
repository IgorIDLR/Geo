package ru.geodesy.adjust.parser;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.common.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбор массива точек сети из JSON-узла.
 * <p>
 * Поддерживает три способа задания ковариационной матрицы:
 * <ul>
 *   <li>{@code covariance}: полная 3×3</li>
 *   <li>{@code sigmaCoord}: одно число (изотропная)</li>
 *   <li>{@code sigmaX / sigmaY / sigmaZ}: раздельные СКО</li>
 * </ul>
 */
@Component
public class PointParser {

    /**
     * Разбирает массив точек.
     */
    public List<Point> parse(JsonNode pointsNode) {
        if (pointsNode == null || !pointsNode.isArray()) {
            throw new ParserException("Список точек отсутствует или не является массивом");
        }

        List<Point> points = new ArrayList<>(pointsNode.size());
        int index = 0;
        for (JsonNode node : pointsNode) {
            try {
                points.add(parsePoint(node));
            } catch (Exception e) {
                throw new ParserException("Ошибка в точке #" + index + ": " + e.getMessage(), e);
            }
            index++;
        }

        if (points.isEmpty()) {
            throw new ParserException("Список точек пуст");
        }
        return points;
    }

    /**
     * Разбирает одну точку.
     */
    public Point parsePoint(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new ParserException("Точка должна быть JSON-объектом");
        }

        String id = Preconditions.requireNonBlank(
                textOrNull(node, "id"), "point.id");

        boolean fixed = node.has("fixed") && node.get("fixed").asBoolean(false);

        Double x = numberOrNull(node, "x");
        Double y = numberOrNull(node, "y");
        Double z = numberOrNull(node, "z");

        double[][] cov = parseCovariance(node);
        return new Point(id, fixed, x, y, z, cov);
    }

    // ============================================================
    // Ковариация
    // ============================================================

    private double[][] parseCovariance(JsonNode node) {
        if (hasNonNull(node, "covariance")) {
            return parseFullCovariance(node.get("covariance"));
        }
        if (hasNonNull(node, "sigmaCoord")) {
            double s = node.get("sigmaCoord").asDouble();
            double v = s * s;
            return new double[][]{
                    {v, 0, 0},
                    {0, v, 0},
                    {0, 0, v}
            };
        }
        if (hasNonNull(node, "sigmaX") || hasNonNull(node, "sigmaY")
                || hasNonNull(node, "sigmaZ")) {
            double sx = numberOrDefault(node, "sigmaX", 0.0);
            double sy = numberOrDefault(node, "sigmaY", 0.0);
            double sz = numberOrDefault(node, "sigmaZ", 0.0);
            return new double[][]{
                    {sx * sx, 0, 0},
                    {0, sy * sy, 0},
                    {0, 0, sz * sz}
            };
        }
        return null;
    }

    private double[][] parseFullCovariance(JsonNode covNode) {
        if (!covNode.isArray() || covNode.size() != 3) {
            throw new ParserException("covariance должна быть массивом 3×3");
        }
        double[][] cov = new double[3][3];
        for (int i = 0; i < 3; i++) {
            JsonNode row = covNode.get(i);
            if (!row.isArray() || row.size() != 3) {
                throw new ParserException("Строка covariance #" + i + " должна иметь 3 элемента");
            }
            for (int j = 0; j < 3; j++) {
                cov[i][j] = row.get(j).asDouble();
            }
        }
        return cov;
    }

    // ============================================================
    // Утилиты
    // ============================================================

    static boolean hasNonNull(JsonNode node, String key) {
        return node.has(key) && !node.get(key).isNull();
    }

    static String textOrNull(JsonNode node, String key) {
        return hasNonNull(node, key) ? node.get(key).asText() : null;
    }

    static Double numberOrNull(JsonNode node, String key) {
        return hasNonNull(node, key) ? node.get(key).asDouble() : null;
    }

    static double numberOrDefault(JsonNode node, String key, double def) {
        return hasNonNull(node, key) ? node.get(key).asDouble() : def;
    }
}