package ru.geodesy.adjust.deformation.api;

import ru.geodesy.common.AdjustmentException;
import ru.geodesy.common.GeodesyException;

public class DeformationException extends AdjustmentException {
    public DeformationException(String msg) { super(msg); }
    public DeformationException(String msg, Throwable cause) { super(msg, cause); }
}