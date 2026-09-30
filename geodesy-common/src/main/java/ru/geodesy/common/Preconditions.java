package ru.geodesy.common;

import java.util.Collection;

/**
 * Утилиты для проверки предусловий. Аналог Guava Preconditions,
 * но без внешних зависимостей.
 */
public final class Preconditions {

    private Preconditions() {
        throw new AssertionError("Utility class");
    }

    /**
     * Проверяет, что условие истинно.
     */
    public static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }

    /**
     * Проверяет, что объект не {@code null}.
     */
    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw ValidationException.missingField(fieldName);
        }
        return value;
    }

    /**
     * Проверяет, что строка не пуста.
     */
    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw ValidationException.missingField(fieldName);
        }
        return value;
    }

    /**
     * Проверяет, что значение положительно (> 0).
     */
    public static double requirePositive(double value, String fieldName) {
        if (!(value > 0)) {
            throw ValidationException.invalidValue(fieldName, value);
        }
        return value;
    }

    /**
     * Проверяет, что значение неотрицательно (≥ 0).
     */
    public static double requireNonNegative(double value, String fieldName) {
        if (!(value >= 0)) {
            throw ValidationException.invalidValue(fieldName, value);
        }
        return value;
    }

    /**
     * Проверяет, что значение конечно (не NaN, не Infinity).
     */
    public static double requireFinite(double value, String fieldName) {
        if (!Double.isFinite(value)) {
            throw ValidationException.invalidValue(fieldName, value);
        }
        return value;
    }

    /**
     * Проверяет, что значение лежит в диапазоне [min, max].
     */
    public static double requireInRange(double value, double min, double max, String fieldName) {
        if (value < min || value > max) {
            throw new ValidationException(
                    "Значение '" + fieldName + "' = " + value
                            + " вне диапазона [" + min + ", " + max + "]");
        }
        return value;
    }

    /**
     * Проверяет, что коллекция не пуста.
     */
    public static <T extends Collection<?>> T requireNonEmpty(T collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new ValidationException("Коллекция '" + fieldName + "' пуста");
        }
        return collection;
    }
}