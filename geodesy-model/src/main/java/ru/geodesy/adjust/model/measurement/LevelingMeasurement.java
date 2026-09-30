package ru.geodesy.adjust.model.measurement;

import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.common.Preconditions;

import java.util.Objects;

/**
 * Превышение между двумя точками (результат нивелирования).
 * Знак: превышение {@code to} относительно {@code from} — {@code H_to − H_from}.
 */
public final class LevelingMeasurement implements SparseMeasurement {

    private final double observed;
    private final double variance;
    private final String fromPoint;
    private final String toPoint;
    private final ParameterSet params;
    private final String accuracyClass;
    private final String instrument;
    private final String tag;

    public LevelingMeasurement(double observed, double variance,
                               String fromPoint, String toPoint,
                               ParameterSet params) {
        this(observed, variance, fromPoint, toPoint, params, null, null, null);
    }

    public LevelingMeasurement(double observed, double variance,
                               String fromPoint, String toPoint,
                               ParameterSet params,
                               String accuracyClass, String instrument, String tag) {
        this.observed = observed;
        this.variance = Preconditions.requirePositive(variance, "leveling.variance");
        this.fromPoint = Preconditions.requireNonBlank(fromPoint, "leveling.from");
        this.toPoint = Preconditions.requireNonBlank(toPoint, "leveling.to");
        this.params = Preconditions.requireNonNull(params, "leveling.params");
        this.accuracyClass = accuracyClass;
        this.instrument = instrument;
        this.tag = tag;

        if (fromPoint.equals(toPoint)) {
            throw new IllegalArgumentException(
                    "Превышение измерено между одной и той же точкой: " + fromPoint);
        }
    }

    public String fromPoint() { return fromPoint; }
    public String toPoint() { return toPoint; }

    @Override public double getObserved() { return observed; }
    @Override public double getVariance() { return variance; }
    @Override public String[] involvedPointIds() { return new String[]{fromPoint, toPoint}; }
    @Override public String accuracyClass() { return accuracyClass; }
    @Override public String instrument() { return instrument; }
    @Override public String tag() { return tag; }

    @Override
    public double computeValue(double[] X) {
        int fi = params.indexOf(fromPoint, 2);
        int ti = params.indexOf(toPoint, 2);
        return X[ti] - X[fi];
    }

    @Override
    public void derivatives(double[] X, double[] derivatives) {
        int fi = params.indexOf(fromPoint, 2);
        int ti = params.indexOf(toPoint, 2);
        derivatives[fi] = -1.0;
        derivatives[ti] =  1.0;
    }

    @Override
    public int sparseDerivatives(double[] X, int[] indices, double[] values) {
        indices[0] = params.indexOf(fromPoint, 2); values[0] = -1.0;
        indices[1] = params.indexOf(toPoint, 2);   values[1] =  1.0;
        return 2;
    }

    @Override
    public int maxSparseSize() { return 2; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LevelingMeasurement other)) return false;
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
        return "Leveling{" + fromPoint + "→" + toPoint
                + ", obs=" + observed
                + ", σ²=" + variance + "}";
    }
}