package ru.geodesy.adjust.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Аннотация валидации точки: проверяет id, координаты и
 * ковариационную матрицу.
 */
@Documented
@Constraint(validatedBy = PointValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPoint {

    String message() default "Некорректная точка";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}