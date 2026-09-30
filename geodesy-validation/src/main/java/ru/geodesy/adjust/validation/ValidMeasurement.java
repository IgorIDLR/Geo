package ru.geodesy.adjust.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Аннотация валидации измерения: проверяет дисперсию,
 * наличие обязательных точек и их отличие.
 */
@Documented
@Constraint(validatedBy = MeasurementValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMeasurement {

    String message() default "Некорректное измерение";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}