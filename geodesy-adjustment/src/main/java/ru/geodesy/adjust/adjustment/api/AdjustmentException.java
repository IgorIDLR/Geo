package ru.geodesy.adjust.adjustment.api;

import ru.geodesy.common.GeodesyException;

public class AdjustmentException extends ru.geodesy.common.AdjustmentException {
    public AdjustmentException(String message) { super(message); }
    public AdjustmentException(String message, Throwable cause) { super(message, cause); }
}