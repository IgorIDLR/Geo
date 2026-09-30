package ru.geodesy.adjust.adjustment.api;

/**
 * Настройки одного запуска уравнивания.
 */
public record AdjustmentOptions(
        double convergenceEps,
        int maxIterations,
        boolean applyDamping,
        boolean computeCovariance,
        boolean detectBlunders,
        double blunderAlpha,
        double blunderBeta
) {

    public static AdjustmentOptions defaults() {
        return new AdjustmentOptions(1e-6, 100, true, true, true, 0.001, 0.80);
    }

    public AdjustmentOptions {
        if (convergenceEps <= 0) convergenceEps = 1e-6;
        if (maxIterations <= 0) maxIterations = 100;
    }
}