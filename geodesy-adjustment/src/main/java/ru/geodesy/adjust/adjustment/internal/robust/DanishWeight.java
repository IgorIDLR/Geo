package ru.geodesy.adjust.adjustment.internal.robust;

public final class DanishWeight implements WeightFunction {
    private final double c;
    public DanishWeight(double c) { this.c = c; }
    public DanishWeight() { this(2.0); }

    @Override
    public double compute(double residual, double sigma) {
        double u = Math.abs(residual) / sigma;
        return u <= c ? 1.0 : Math.exp(-(u - c) * u);
    }
}