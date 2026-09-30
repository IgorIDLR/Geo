package ru.geodesy.common;

import java.io.Serial;

/**
 * Базовое проверяемое-нет исключение проекта.
 * <p>
 * Все исключения проекта наследуются от этого класса.
 * Помечено {@code sealed}, чтобы контролировать возможные подтипы:
 * новые исключения должны явно объявляться в {@code permits}.
 * <p>
 * <b>Примечание:</b> несмотря на {@code sealed}, исключение остаётся
 * непроверяемым (наследник {@link RuntimeException}), так как в
 * вычислительных задачах проверяемые исключения создают избыточный шум.
 */
public sealed class GeodesyException extends RuntimeException
        permits AdjustmentException, ConvergenceException, InsufficientDataException, SingularMatrixException, ValidationException {

    @Serial
    private static final long serialVersionUID = 1L;

    public GeodesyException(String message) {
        super(message);
    }

    public GeodesyException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeodesyException(Throwable cause) {
        super(cause);
    }
}