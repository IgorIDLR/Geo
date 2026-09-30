package ru.geodesy.adjust.session.api;

import ru.geodesy.common.AdjustmentException;

public class SessionException extends AdjustmentException {
    public SessionException(String msg) { super(msg); }
    public SessionException(String msg, Throwable cause) { super(msg, cause); }
}