package ru.geodesy.adjust.model.measurement;

import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.common.Preconditions;

import java.util.Objects;

/**
 * Дирекционный угол (азимут) стороны {@code from→to}.
 */
public final class AzimuthMeasurement implements SparseMeasurement {

    private final double observed;
    private final double variance;
    private final String fromPoint;
    private final String toPoint;
    private final ParameterSet params;

    public AzimuthMeasurement(double observed, double variance,
                              String fromPoint, String toPoint,
                              ParameterSet params) {
        this.observed = observed;
        this.variance = Preconditions.requirePositive(variance, "azimuth.variance");
        this.fromPoint = Preconditions.requireNonBlank(fromPoint, "azimuth.from");
        this.toPoint = Preconditions.requireNonBlank(toPoint, "azimuth.to");
        this.params = Preconditions.requireNonNull(params, "azimuth.params");
    }

    public String fromPoint() { return fromPoint; }
    public String toPoint() { return toPoint; }

    @Override public double getObserved() { return observed; }
    @Override public double getVariance() { return variance; }
    @Override public String[] involvedPointIds() { return new String[]{fromPoint, toPoint}; }

    @Override
    public double computeValue(double[] X) {
        int fi = params.indexOf(fromPoint, 0);
        int ti = params.indexOf(toPoint, 0);
        return Math.atan2(X[ti + 1] - X[fi + 1], X[ti] - X[fi]);
    }

    @Override
    public void derivatives(double[] X, double[] derivatives) {
        int fi = params.indexOf(fromPoint, 0);
        int ti = params.indexOf(toPoint, 0);
        double dx = X[ti] - X[fi];
        double dy = X[ti + 1] - X[fi + 1];
        double denom = dx * dx + dy * dy;
        if (denom < 1e-15) {
            throw new ArithmeticException("Нулевой вектор: " + fromPoint + "→" + toPoint);
        }
        double a = -dy / denom;
        double b =  dx / denom;
        derivatives[fi]     = -a;
        derivatives[fi + 1] = -b;
        derivatives[ti]     =  a;
        derivatives[ti + 1] =  b;
    }

    @Override
    public int sparseDerivatives(double[] X, int[] indices, double[] values) {
        int fi = params.indexOf(fromPoint, 0);
        int ti = params.indexOf(toPoint, 0);
        double dx = X[ti] - X[fi];
        double dy = X[ti + 1] - X[fi + 1];
        double denom = dx * dx + dy * dy;
        if (denom < 1e-15) {
            throw new ArithmeticException("Нулевой вектор: " + fromPoint + "→" + toPoint);
        }
        double a = -dy / denom;
        double b =  dx / denom;
        indices[0] = fi;     values[0] = -a;
        indices[1] = fi + 1; values[1] = -b;
        indices[2] = ti;     values[2] =  a;
        indices[3] = ti + 1; values[3] =  b;
        return 4;
    }

    @Override
    public int maxSparseSize() { return 4; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AzimuthMeasurement other)) return false;
        return Double.compare(observed, other.observed) == 0
                && Double.compare(variance, other.variance) == 0
                && fromPoint.equals(other.fromPoint)
                && toPoint.equals(other.toPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(observed, variance, fromPoint, toPoint);
    }

    @Override
    public String toString() {
        return "Azimuth{" + fromPoint + "→" + toPoint
                + ", obs=" + observed
                + ", σ²=" + variance + "}";
    }
}