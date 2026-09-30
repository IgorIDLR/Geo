package ru.geodesy.adjust.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.Network;

/**
 * Пайплайн валидации: выполняет все виды проверок последовательно.
 * <p>
 * Этапы:
 * <ol>
 *   <li>Jakarta Bean Validation (синтаксис + семантика каждого объекта)</li>
 *   <li>Топологическая валидация сети (связность, дубликаты)</li>
 *   <li>Геометрическая валидация (опционально)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationPipeline {

    private final NetworkIntegrityValidator integrityValidator;
    private final jakarta.validation.Validator beanValidator;

    /**
     * Выполняет все проверки.
     */
    public ValidationResult validate(Network network) {
        ValidationResult result = new ValidationResult();

        // 1. Bean Validation для каждой точки
        for (var point : network.points()) {
            var violations = beanValidator.validate(point);
            for (var v : violations) {
                result.addError(ValidationError.semantic(
                        "point[" + point.id() + "]." + v.getPropertyPath(),
                        v.getMessage()));
            }
        }

        // 2. Bean Validation для каждого измерения
        for (int i = 0; i < network.measurements().size(); i++) {
            var m = network.measurements().get(i);
            var violations = beanValidator.validate(m);
            for (var v : violations) {
                result.addError(ValidationError.semantic(
                        "measurement[" + i + "]." + v.getPropertyPath(),
                        v.getMessage()));
            }
        }

        // 3. Топология
        ValidationResult integrityResult = integrityValidator.validate(network);
        result.merge(integrityResult);

        return result;
    }

    /**
     * Выполняет проверки или выбрасывает исключение.
     */
    public void validateOrThrow(Network network) {
        ValidationResult result = validate(network);
        if (!result.isValid()) {
            throw new ru.geodesy.common.ValidationException(
                    "Сеть не прошла валидацию:\n" + result.describeErrors());
        }
    }
}