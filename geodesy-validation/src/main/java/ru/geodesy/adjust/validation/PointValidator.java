package ru.geodesy.adjust.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.common.MathUtils;

/**
 * Валидатор точки:
 * <ul>
 *   <li>ID не пуст</li>
 *   <li>Координаты, если заданы, конечны</li>
 *   <li>Ковариационная матрица симметрична и положительно определена</li>
 * </ul>
 */
@Slf4j
public class PointValidator implements ConstraintValidator<ValidPoint, Point> {

    @Override
    public boolean isValid(Point point, ConstraintValidatorContext context) {
        if (point == null) {
            addViolation(context, "Точка не может быть null");
            return false;
        }

        boolean valid = true;

        // ID
        if (point.id() == null || point.id().isBlank()) {
            addViolation(context, "ID точки пуст");
            valid = false;
        }

        // Координаты конечны
        if (point.x() != null && !MathUtils.isFinite(point.x())) {
            addViolation(context, "Координата X не конечна");
            valid = false;
        }
        if (point.y() != null && !MathUtils.isFinite(point.y())) {
            addViolation(context, "Координата Y не конечна");
            valid = false;
        }
        if (point.z() != null && !MathUtils.isFinite(point.z())) {
            addViolation(context, "Координата Z не конечна");
            valid = false;
        }

        // Ковариация
        double[][] cov = point.covariance();
        if (cov != null) {
            if (cov.length != 3 || cov[0].length != 3) {
                addViolation(context, "Ковариационная матрица должна быть 3×3");
                valid = false;
            } else {
                // Симметричность
                for (int i = 0; i < 3; i++) {
                    for (int j = i + 1; j < 3; j++) {
                        if (Math.abs(cov[i][j] - cov[j][i]) > 1e-12) {
                            addViolation(context, "Ковариационная матрица несимметрична");
                            valid = false;
                        }
                    }
                }
                // Положительная определённость (приблизительно, через диагональ)
                for (int i = 0; i < 3; i++) {
                    if (cov[i][i] < 0) {
                        addViolation(context, "Диагональный элемент ковариации [" + i + "] отрицателен");
                        valid = false;
                    }
                }
            }
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}