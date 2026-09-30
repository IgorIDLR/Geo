package ru.geodesy.adjust.adjustment.internal.robust;

public final class TukeyWeight implements WeightFunction {
    private final double c;
    public TukeyWeight(double c) { this.c = c; }
    public TukeyWeight() { this(4.685); }

    @Override
    public double compute(double residual, double sigma) {
        double u = Math.abs(residual) / sigma;
        if (u >= c) return 0.0;
        double t = 1 - (u / c) * (u / c);
        return t * t;
    }
}