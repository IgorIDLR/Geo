package ru.geodesy.adjust.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.NetworkType;
import ru.geodesy.adjust.validation.topological.DuplicateDetector;
import ru.geodesy.adjust.validation.topological.GraphValidator;
import ru.geodesy.adjust.validation.topological.IsolatedPointDetector;

import java.util.HashSet;
import java.util.Set;

/**
 * Главный валидатор целостности сети.
 * <p>
 * Выполняет топологическую (связность, изолированные точки, дубликаты)
 * и семантическую (достаточность измерений, наличие опорных точек)
 * проверки.
 */
@Slf4j
@Component
public class NetworkIntegrityValidator {

    private final GraphValidator graphValidator = new GraphValidator();
    private final IsolatedPointDetector isolatedPointDetector = new IsolatedPointDetector();
    private final DuplicateDetector duplicateDetector = new DuplicateDetector();

    /**
     * Валидирует сеть и возвращает результат.
     */
    public ValidationResult validate(Network network) {
        ValidationResult result = new ValidationResult();

        if (network == null) {
            result.addError(ValidationError.semantic("network",
                    "Сеть не может быть null"));
            return result;
        }

        // 1. Изолированные точки
        isolatedPointDetector.detect(network, result);

        // 2. Связность графа
        graphValidator.validate(network, result);

        // 3. Дубликаты измерений
        duplicateDetector.detect(network.measurements(), result);

        // 4. Семантические проверки
        validateSemantics(network, result);

        if (result.isValid()) {
            log.info("Валидация пройдена: {} точек ({} определяемых), {} измерений",
                    network.points().size(), network.countFreePoints(),
                    network.measurements().size());
        } else {
            log.warn("Валидация не пройдена: {} ошибок", result.errors().size());
        }

        return result;
    }

    /**
     * Валидирует или выбрасывает исключение.
     */
    public void validateOrThrow(Network network) {
        ValidationResult result = validate(network);
        if (!result.isValid()) {
            throw new ru.geodesy.common.ValidationException(
                    "Сеть не прошла валидацию:\n" + result.describeErrors());
        }
    }

    // ================================================================
    // Семантические проверки
    // ================================================================

    private void validateSemantics(Network network, ValidationResult result) {

        // ---------- 1. Достаточность измерений ----------
        // ВАЖНО: параметры считаются ТОЛЬКО по определяемым точкам,
        // координаты фиксированных точек не являются неизвестными
        long freePoints = network.countFreePoints();
        int pointParams = (int) freePoints * network.dimension();
        int measCount = network.measurements().size();
        int defRank = network.networkType().totalDefectRank();

        if (measCount + defRank < pointParams) {
            result.addError(ValidationError.semantic(
                    "network",
                    String.format(
                            "Недостаточно измерений: %d + %d (дефект) < %d "
                                    + "(определяемых точек: %d × %d координат)",
                            measCount, defRank, pointParams,
                            freePoints, network.dimension())));
        }

        // ---------- 2. FIXED-сеть должна иметь хотя бы одну опорную точку ----------
        NetworkType type = network.networkType();
        if (type.planarDefect() == NetworkType.PlanarDefect.ZERO
                && network.countFixedPoints() == 0) {
            result.addError(ValidationError.semantic(
                    "network",
                    "Сеть типа FIXED должна содержать хотя бы одну фиксированную точку"));
        }

        // ---------- 3. Проверка уникальности ID точек ----------
        Set<String> seenIds = new HashSet<>();
        for (var p : network.points()) {
            if (!seenIds.add(p.id())) {
                result.addError(ValidationError.semantic(
                        "point[" + p.id() + "]",
                        "Дублирующийся ID точки"));
            }
        }

        // ---------- 4. Проверка координат фиксированных точек ----------
        if (network.dimension() == 2 || network.dimension() == 3) {
            for (var p : network.points()) {
                if (!p.fixed()) continue;

                if (p.x() == null || p.y() == null) {
                    result.addError(ValidationError.semantic(
                            "point[" + p.id() + "]",
                            "Фиксированная точка не имеет плановых координат"));
                }
                if (network.dimension() == 3 && p.z() == null) {
                    result.addError(ValidationError.semantic(
                            "point[" + p.id() + "]",
                            "Фиксированная точка не имеет высотной координаты"));
                }
            }
        } else if (network.dimension() == 1) {
            for (var p : network.points()) {
                if (p.fixed() && p.z() == null) {
                    result.addError(ValidationError.semantic(
                            "point[" + p.id() + "]",
                            "Фиксированная точка не имеет высоты"));
                }
            }
        }
    }
}