package ru.geodesy.common;

/**
 * Ошибка в процессе уравнивания.
 */
public non-sealed class AdjustmentException extends GeodesyException {
    public AdjustmentException(String message) { super(message); }
    public AdjustmentException(String message, Throwable cause) { super(message, cause); }
}