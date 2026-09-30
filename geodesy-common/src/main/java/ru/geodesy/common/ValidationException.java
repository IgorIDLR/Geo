package ru.geodesy.common;

import java.io.Serial;

/**
 * Ошибка валидации входных данных: некорректный формат,
 * отсутствующие поля, противоречивые параметры.
 */
public non-sealed class ValidationException extends GeodesyException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Создаёт исключение для отсутствующего поля.
     */
    public static ValidationException missingField(String field) {
        return new ValidationException("Обязательное поле отсутствует: " + field);
    }

    /**
     * Создаёт исключение для некорректного значения поля.
     */
    public static ValidationException invalidValue(String field, Object value) {
        return new ValidationException(
                "Некорректное значение поля '" + field + "': " + value);
    }
}