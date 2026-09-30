package ru.geodesy.adjust.adjustment.api;

/**
 * Настройки объёма выходных данных в ответе.
 */
public record OutputOptions(
        boolean coordinates,
        boolean covariance,
        boolean blunders,
        boolean relativeErrors,
        boolean confidenceIntervals,
        boolean toleranceCheck
) {
    /** Вывод всех полей. */
    public static final OutputOptions FULL = new OutputOptions(
            true, true, true, true, true, true);

    /** Минимальный вывод — только координаты. */
    public static final OutputOptions MINIMAL = new OutputOptions(
            true, false, false, false, false, false);
}