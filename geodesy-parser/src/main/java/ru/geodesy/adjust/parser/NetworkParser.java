package ru.geodesy.adjust.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.*;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.common.Constants;
import ru.geodesy.common.Preconditions;

import java.util.List;

/**
 * Главный парсер: JSON-строка → {@link Network}.
 * <p>
 * Автоматически определяет формат:
 * <ul>
 *   <li>Стандартный — объект с ключами {@code points}, {@code measurements}</li>
 *   <li>Упрощённый TRAVERSE — передаётся в {@link TraverseParser}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NetworkParser {

    private final ObjectMapper objectMapper;
    private final PointParser pointParser;
    private final MeasurementParser measurementParser;
    private final TraverseParser traverseParser;

    /**
     * Разбирает JSON-описание сети.
     *
     * @param raw строка с JSON
     * @return доменная модель сети
     * @throws ParserException при некорректных данных
     */
    public Network parse(String raw) {
        Preconditions.requireNonBlank(raw, "network.rawData");

        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new ParserException("Не удалось разобрать JSON: " + e.getMessage(), e);
        }

        if (root == null || !root.isObject()) {
            throw new ParserException("Корневой элемент JSON должен быть объектом");
        }

        // TRAVERSE-формат
        String type = PointParser.textOrNull(root, ParserConstants.KEY_TYPE);
        if (ParserConstants.VALUE_TRAVERSE.equalsIgnoreCase(type)) {
            log.debug("Обнаружен TRAVERSE-формат");
            return traverseParser.parse(root);
        }

        // Стандартный формат
        return parseStandard(root);
    }

    private Network parseStandard(JsonNode root) {
        List<Point> points = pointParser.parse(root.get(ParserConstants.KEY_POINTS));
        ParameterSet params = ParameterSet.fromPoints(points);
        List<Measurement> measurements = measurementParser.parse(
                root.get(ParserConstants.KEY_MEASUREMENTS), params);

        NetworkType networkType = parseNetworkType(root);
        double sigma0 = PointParser.numberOrDefault(
                root, ParserConstants.KEY_SIGMA0, Constants.DEFAULT_SIGMA0);
        int dimension = (int) PointParser.numberOrDefault(
                root, ParserConstants.KEY_DIMENSION, 2);

        validateReferences(points, measurements);

        log.info("Сеть разобрана: {} точек, {} измерений, тип = {}, sigma0 = {}",
                points.size(), measurements.size(), networkType, sigma0);

        return new Network(points, measurements, networkType, sigma0, dimension);
    }

    private NetworkType parseNetworkType(JsonNode root) {
        String typeStr = PointParser.textOrNull(root, ParserConstants.KEY_NETWORK_TYPE);
        if (typeStr == null || typeStr.isBlank()) {
            return NetworkType.fixed();
        }
        return switch (typeStr.toUpperCase()) {
            case "FIXED" -> NetworkType.fixed();
            case "FREE" -> NetworkType.free2D();
            case "UNCONNECTED" -> NetworkType.unconnected2D();
            case "FREE3D" -> NetworkType.free3D();
            default -> throw new ParserException("Неизвестный тип сети: " + typeStr);
        };
    }

    /**
     * Проверяет, что все точки, упомянутые в измерениях, есть в списке точек.
     */
    private void validateReferences(List<Point> points, List<Measurement> measurements) {
        var ids = new java.util.HashSet<String>();
        for (Point p : points) ids.add(p.id());

        for (Measurement m : measurements) {
            for (String id : m.involvedPointIds()) {
                if (!ids.contains(id)) {
                    throw new ParserException(
                            "Измерение ссылается на неизвестную точку: " + id);
                }
            }
        }
    }
}