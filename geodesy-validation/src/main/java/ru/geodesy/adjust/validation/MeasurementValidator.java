package ru.geodesy.adjust.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.geodesy.adjust.model.measurement.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Валидатор измерения:
 * <ul>
 *   <li>Дисперсия положительна и конечна</li>
 *   <li>Задействованные точки заданы</li>
 *   <li>Для измерений "from→to" точки различаются</li>
 * </ul>
 */
public class MeasurementValidator implements ConstraintValidator<ValidMeasurement, Measurement> {

    @Override
    public boolean isValid(Measurement measurement, ConstraintValidatorContext context) {
        if (measurement == null) {
            addViolation(context, "Измерение не может быть null");
            return false;
        }

        boolean valid = true;

        // Дисперсия
        double variance = measurement.getVariance();
        if (!(variance > 0) || !Double.isFinite(variance)) {
            addViolation(context, "Дисперсия должна быть положительной и конечной: " + variance);
            valid = false;
        }

        // Идентификаторы точек
        String[] ids = measurement.involvedPointIds();
        if (ids == null || ids.length == 0) {
            addViolation(context, "Измерение не содержит точек");
            return false;
        }
        Set<String> uniqueIds = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                addViolation(context, "ID точки пуст в измерении");
                valid = false;
            } else if (!uniqueIds.add(id)) {
                addViolation(context, "Дублирующаяся точка в измерении: " + id);
                valid = false;
            }
        }

        // Специфичные проверки
        if (measurement instanceof DistanceMeasurement dm) {
            if (dm.fromPoint().equals(dm.toPoint())) {
                addViolation(context, "Расстояние между одной и той же точкой");
                valid = false;
            }
        } else if (measurement instanceof LevelingMeasurement lm) {
            if (lm.fromPoint().equals(lm.toPoint())) {
                addViolation(context, "Превышение между одной и той же точкой");
                valid = false;
            }
        } else if (measurement instanceof HorizontalAngleMeasurement am) {
            if (am.leftPoint().equals(am.rightPoint())) {
                addViolation(context, "Угол между совпадающими направлениями");
                valid = false;
            }
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}