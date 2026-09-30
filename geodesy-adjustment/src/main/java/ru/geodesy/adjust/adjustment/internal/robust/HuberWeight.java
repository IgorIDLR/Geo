package ru.geodesy.adjust.adjustment.internal.robust;

public final class HuberWeight implements WeightFunction {
    private final double c;
    public HuberWeight(double c) { this.c = c; }
    public HuberWeight() { this(1.345); }

    @Override
    public double compute(double residual, double sigma) {
        double u = Math.abs(residual) / sigma;
        return u <= c ? 1.0 : c / u;
    }
}