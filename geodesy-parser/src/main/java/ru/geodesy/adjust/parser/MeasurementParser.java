package ru.geodesy.adjust.parser;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.*;
import ru.geodesy.common.AngleUtils;
import ru.geodesy.common.Preconditions;

import java.util.ArrayList;
import java.util.List;

import static ru.geodesy.adjust.parser.PointParser.numberOrDefault;
import static ru.geodesy.adjust.parser.PointParser.textOrNull;

/**
 * Разбор массива измерений.
 * <p>
 * Поддерживаемые типы:
 * <ul>
 *   <li>{@code DISTANCE} — расстояние</li>
 *   <li>{@code ANGLE} — горизонтальный угол</li>
 *   <li>{@code AZIMUTH} — дирекционный угол</li>
 *   <li>{@code LEVELING} — превышение</li>
 * </ul>
 * <p>
 * Углы могут задаваться в трёх форматах:
 * <ul>
 *   <li>число — радианы</li>
 *   <li>строка — DMS ("45°30′15.2″")</li>
 *   <li>объект — {@code {"d":45,"m":30,"s":15.2}}</li>
 * </ul>
 */
@Component
public class MeasurementParser {

    /**
     * Разбирает массив измерений.
     */
    public List<Measurement> parse(JsonNode measurementsNode, ParameterSet params) {
        if (measurementsNode == null || !measurementsNode.isArray()) {
            throw new ParserException(
                    "Список измерений отсутствует или не является массивом");
        }

        List<Measurement> measurements = new ArrayList<>(measurementsNode.size());
        int index = 0;
        for (JsonNode node : measurementsNode) {
            try {
                measurements.add(parseMeasurement(node, params));
            } catch (ParserException e) {
                throw e;
            } catch (Exception e) {
                throw new ParserException(
                        "Ошибка в измерении #" + index + ": " + e.getMessage(), e);
            }
            index++;
        }

        if (measurements.isEmpty()) {
            throw new ParserException("Список измерений пуст");
        }
        return measurements;
    }

    /**
     * Разбирает одно измерение.
     */
    public Measurement parseMeasurement(JsonNode node, ParameterSet params) {
        if (node == null || !node.isObject()) {
            throw new ParserException("Измерение должно быть JSON-объектом");
        }

        String type = requireText(node, "type");
        double observed = parseObserved(node.get("observed"));
        double variance = numberOrDefault(node, "variance", 1.0);

        String accuracyClass = textOrNull(node, "class");
        String instrument = textOrNull(node, "instrument");
        String tag = textOrNull(node, "tag");

        return switch (type.toUpperCase()) {
            case "DISTANCE" -> new DistanceMeasurement(
                    observed, variance,
                    requireText(node, "from"),
                    requireText(node, "to"),
                    params, accuracyClass, instrument, tag);
            case "ANGLE" -> new HorizontalAngleMeasurement(
                    observed, variance,
                    requireText(node, "left"),
                    requireText(node, "top"),
                    requireText(node, "right"),
                    params, accuracyClass, instrument, tag);
            case "AZIMUTH" -> new AzimuthMeasurement(
                    observed, variance,
                    requireText(node, "from"),
                    requireText(node, "to"),
                    params);
            case "LEVELING" -> new LevelingMeasurement(
                    observed, variance,
                    requireText(node, "from"),
                    requireText(node, "to"),
                    params, accuracyClass, instrument, tag);
            default -> throw new ParserException(
                    "Неизвестный тип измерения: " + type);
        };
    }

    // ============================================================
    // Разбор observed
    // ============================================================

    /**
     * Разбирает значение {@code observed} в зависимости от типа узла:
     * число, объект DMS или строка DMS.
     */
    public double parseObserved(JsonNode node) {
        if (node == null || node.isNull()) {
            throw new ParserException("Поле observed обязательно");
        }
        if (node.isNumber()) return node.asDouble();
        if (node.isObject()) {
            double d = numberOrDefault(node, "d", 0);
            double m = numberOrDefault(node, "m", 0);
            double s = numberOrDefault(node, "s", 0);
            return AngleUtils.dmsToRadians(d, m, s);
        }
        if (node.isTextual()) {
            try {
                return AngleUtils.parseDmsToRadians(node.asText());
            } catch (Exception e) {
                throw new ParserException(
                        "Не удалось разобрать угол: " + node.asText(), e);
            }
        }
        throw new ParserException(
                "Неподдерживаемый формат observed: " + node.getNodeType());
    }

    // ============================================================
    // Утилиты
    // ============================================================

    /**
     * Извлекает обязательное строковое поле или выбрасывает ParserException.
     * <p>
     * ВАЖНО: оборачивает ValidationException в ParserException, чтобы
     * отличать ошибки разбора от семантических ошибок.
     */
    private String requireText(JsonNode node, String key) {
        if (node == null) {
            throw new ParserException(
                    "Отсутствует поле '" + key + "'");
        }
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            throw new ParserException(
                    "Обязательное поле отсутствует: " + key);
        }
        String text = value.asText();
        if (text == null || text.isBlank()) {
            throw new ParserException(
                    "Обязательное поле пустое: " + key);
        }
        return text;
    }

    private String textOrNull(JsonNode node, String key) {
        if (node == null) return null;
        JsonNode value = node.get(key);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private double numberOrDefault(JsonNode node, String key, double def) {
        if (node == null) return def;
        JsonNode value = node.get(key);
        return value != null && !value.isNull() ? value.asDouble() : def;
    }
}