package ru.geodesy.adjust.model.measurement;

import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.common.Preconditions;

import java.util.Objects;

/**
 * Линейное измерение — горизонтальное проложение между двумя точками.
 * Поддерживает разреженные производные (4 ненулевых элемента).
 */
public final class DistanceMeasurement implements SparseMeasurement {

    private final double observed;
    private final double variance;
    private final String fromPoint;
    private final String toPoint;
    private final ParameterSet params;
    private final String accuracyClass;
    private final String instrument;
    private final String tag;

    public DistanceMeasurement(double observed, double variance,
                               String fromPoint, String toPoint,
                               ParameterSet params) {
        this(observed, variance, fromPoint, toPoint, params, null, null, null);
    }

    public DistanceMeasurement(double observed, double variance,
                               String fromPoint, String toPoint,
                               ParameterSet params,
                               String accuracyClass, String instrument, String tag) {
        this.observed = observed;
        this.variance = Preconditions.requirePositive(variance, "distance.variance");
        this.fromPoint = Preconditions.requireNonBlank(fromPoint, "distance.from");
        this.toPoint = Preconditions.requireNonBlank(toPoint, "distance.to");
        this.params = Preconditions.requireNonNull(params, "distance.params");
        this.accuracyClass = accuracyClass;
        this.instrument = instrument;
        this.tag = tag;

        if (fromPoint.equals(toPoint)) {
            throw new IllegalArgumentException(
                    "Расстояние измерено между одной и той же точкой: " + fromPoint);
        }
    }

    // ============================================================
    // Геттеры
    // ============================================================

    public String fromPoint() { return fromPoint; }
    public String toPoint() { return toPoint; }

    @Override public double getObserved() { return observed; }
    @Override public double getVariance() { return variance; }
    @Override public String[] involvedPointIds() { return new String[]{fromPoint, toPoint}; }
    @Override public String accuracyClass() { return accuracyClass; }
    @Override public String instrument() { return instrument; }
    @Override public String tag() { return tag; }

    // ============================================================
    // Вычисления
    // ============================================================

    @Override
    public double computeValue(double[] X) {
        int fi = params.indexOf(fromPoint, 0);
        int ti = params.indexOf(toPoint, 0);
        double dx = X[ti] - X[fi];
        double dy = X[ti + 1] - X[fi + 1];
        return Math.hypot(dx, dy);
    }

    @Override
    public void derivatives(double[] X, double[] derivatives) {
        int fi = params.indexOf(fromPoint, 0);
        int ti = params.indexOf(toPoint, 0);
        double dx = X[ti] - X[fi];
        double dy = X[ti + 1] - X[fi + 1];
        double d = Math.hypot(dx, dy);
        if (d < 1e-12) {
            throw new ArithmeticException(
                    "Нулевое расстояние между " + fromPoint + " и " + toPoint);
        }
        derivatives[fi]     = -dx / d;
        derivatives[fi + 1] = -dy / d;
        derivatives[ti]     =  dx / d;
        derivatives[ti + 1] =  dy / d;
    }

    @Override
    public int sparseDerivatives(double[] X, int[] indices, double[] values) {
        int fi = params.indexOf(fromPoint, 0);
        int ti = params.indexOf(toPoint, 0);
        double dx = X[ti] - X[fi];
        double dy = X[ti + 1] - X[fi + 1];
        double d = Math.hypot(dx, dy);
        if (d < 1e-12) {
            throw new ArithmeticException(
                    "Нулевое расстояние между " + fromPoint + " и " + toPoint);
        }
        indices[0] = fi;     values[0] = -dx / d;
        indices[1] = fi + 1; values[1] = -dy / d;
        indices[2] = ti;     values[2] =  dx / d;
        indices[3] = ti + 1; values[3] =  dy / d;
        return 4;
    }

    @Override
    public int maxSparseSize() { return 4; }

    // ============================================================
    // equals / hashCode / toString
    // ============================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DistanceMeasurement other)) return false;
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
        return "Distance{" + fromPoint + "→" + toPoint
                + ", obs=" + observed
                + ", σ²=" + variance + "}";
    }
}