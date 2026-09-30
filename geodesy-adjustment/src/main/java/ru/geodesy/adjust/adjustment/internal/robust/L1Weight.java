package ru.geodesy.adjust.adjustment.internal.robust;

public final class L1Weight implements WeightFunction {
    private static final double EPS = 1e-8;

    @Override
    public double compute(double residual, double sigma) {
        return 1.0 / (Math.abs(residual) / sigma + EPS);
    }
}