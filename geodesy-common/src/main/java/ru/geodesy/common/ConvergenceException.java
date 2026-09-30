package ru.geodesy.common;

import java.io.Serial;

/**
 * Итерационный процесс не сошёлся за отведённое число итераций
 * или сходимость невозможна (расходимость, осцилляция).
 */
public final class ConvergenceException extends GeodesyException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int iterations;
    private final double lastDelta;

    public ConvergenceException(String message, int iterations, double lastDelta) {
        super(message);
        this.iterations = iterations;
        this.lastDelta = lastDelta;
    }

    public ConvergenceException(String message) {
        this(message, 0, Double.NaN);
    }

    public int getIterations() {
        return iterations;
    }

    public double getLastDelta() {
        return lastDelta;
    }
}