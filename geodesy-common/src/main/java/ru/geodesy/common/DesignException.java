package ru.geodesy.common;

/**
 * Ошибка в процессе проектирования точности.
 */
public class DesignException extends AdjustmentException {
    public DesignException(String message) { super(message); }
    public DesignException(String message, Throwable cause) { super(message, cause); }
}