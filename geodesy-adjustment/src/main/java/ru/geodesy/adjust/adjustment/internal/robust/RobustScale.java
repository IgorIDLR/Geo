package ru.geodesy.adjust.adjustment.internal.robust;

import ru.geodesy.common.MathUtils;

/**
 * Робастная оценка масштаба через MAD.
 */
public final class RobustScale {
    private RobustScale() {}

    public static double mad(double[] values) {
        return MathUtils.mad(values);
    }
}