package ru.geodesy.adjust.model.measurement;

import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.common.Preconditions;

import java.util.Objects;

/**
 * Горизонтальный угол между двумя направлениями с вершиной в {@code top}.
 * Измеряется от направления {@code top→left} до {@code top→right}
 * по часовой стрелке (левосторонний угол).
 * <p>
 * Производные вычисляются численно (центральные разности).
 */
public final class HorizontalAngleMeasurement implements SparseMeasurement {

    private static final double DERIVATIVE_STEP = 1e-8;

    private final double observed;
    private final double variance;
    private final String leftPoint;
    private final String topPoint;
    private final String rightPoint;
    private final ParameterSet params;
    private final String accuracyClass;
    private final String instrument;
    private final String tag;

    public HorizontalAngleMeasurement(double observed, double variance,
                                      String leftPoint, String topPoint,
                                      String rightPoint, ParameterSet params) {
        this(observed, variance, leftPoint, topPoint, rightPoint, params,
                null, null, null);
    }

    public HorizontalAngleMeasurement(double observed, double variance,
                                      String leftPoint, String topPoint,
                                      String rightPoint, ParameterSet params,
                                      String accuracyClass, String instrument, String tag) {
        this.observed = observed;
        this.variance = Preconditions.requirePositive(variance, "angle.variance");
        this.leftPoint = Preconditions.requireNonBlank(leftPoint, "angle.left");
        this.topPoint = Preconditions.requireNonBlank(topPoint, "angle.top");
        this.rightPoint = Preconditions.requireNonBlank(rightPoint, "angle.right");
        this.params = Preconditions.requireNonNull(params, "angle.params");
        this.accuracyClass = accuracyClass;
        this.instrument = instrument;
        this.tag = tag;
    }

    // ============================================================
    // Геттеры
    // ============================================================

    public String leftPoint() { return leftPoint; }
    public String topPoint() { return topPoint; }
    public String rightPoint() { return rightPoint; }

    @Override public double getObserved() { return observed; }
    @Override public double getVariance() { return variance; }
    @Override public String[] involvedPointIds() {
        return new String[]{leftPoint, topPoint, rightPoint};
    }
    @Override public String accuracyClass() { return accuracyClass; }
    @Override public String instrument() { return instrument; }
    @Override public String tag() { return tag; }

    // ============================================================
    // Вычисления
    // ============================================================

    @Override
    public double computeValue(double[] X) {
        int li = params.indexOf(leftPoint, 0);
        int ti = params.indexOf(topPoint, 0);
        int ri = params.indexOf(rightPoint, 0);

        double ax = X[li] - X[ti];
        double ay = X[li + 1] - X[ti + 1];
        double bx = X[ri] - X[ti];
        double by = X[ri + 1] - X[ti + 1];

        double angle = Math.atan2(by, bx) - Math.atan2(ay, ax);
        return normalizeTo2Pi(angle);
    }

    @Override
    public void derivatives(double[] X, double[] derivatives) {
        double h = DERIVATIVE_STEP;
        int n = X.length;
        for (int i = 0; i < n; i++) {
            double old = X[i];

            X[i] = old + h;
            double plus = computeValue(X);

            X[i] = old - h;
            double minus = computeValue(X);

            derivatives[i] = (plus - minus) / (2 * h);
            X[i] = old;
        }
    }

    @Override
    public int sparseDerivatives(double[] X, int[] indices, double[] values) {
        // Угол зависит от 6 координат (2 точки × 3 координаты каждая)
        // плюс top тоже может участвовать — итого 6 элементов (без Z)
        double[] full = new double[X.length];
        derivatives(X, full);
        int k = 0;
        for (int i = 0; i < full.length; i++) {
            if (Math.abs(full[i]) > 1e-12) {
                indices[k] = i;
                values[k] = full[i];
                k++;
            }
        }
        return k;
    }

    @Override
    public int maxSparseSize() { return 6; }

    // ============================================================
    // Утилиты
    // ============================================================

    private static double normalizeTo2Pi(double angle) {
        double result = angle % (2 * Math.PI);
        return result < 0 ? result + 2 * Math.PI : result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HorizontalAngleMeasurement other)) return false;
        return Double.compare(observed, other.observed) == 0
                && Double.compare(variance, other.variance) == 0
                && leftPoint.equals(other.leftPoint)
                && topPoint.equals(other.topPoint)
                && rightPoint.equals(other.rightPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(observed, variance, leftPoint, topPoint, rightPoint);
    }

    @Override
    public String toString() {
        return "Angle{" + leftPoint + "-" + topPoint + "-" + rightPoint
                + ", obs=" + observed
                + ", σ²=" + variance + "}";
    }
}