package ru.geodesy.adjust.model.measurement;

import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.common.Preconditions;

/**
 * Векторное измерение априорных координат точки с полной
 * ковариационной матрицей 3×3. Учитывает корреляции между
 * X, Y, Z (в отличие от скалярного варианта).
 * <p>
 * Используется при уравнивании с учётом ошибок исходных данных.
 */
public final class PriorCoordinateBlockMeasurement implements BlockMeasurement {

    private final String pointId;
    private final double[] observed;
    private final SimpleMatrix covariance;
    private final ParameterSet params;
    private final int baseIndex;

    public PriorCoordinateBlockMeasurement(String pointId,
                                           double x, double y, double z,
                                           double[][] covMatrix,
                                           ParameterSet params) {
        this.pointId = Preconditions.requireNonBlank(pointId, "prior.pointId");
        this.observed = new double[]{x, y, z};
        this.covariance = new SimpleMatrix(
                Preconditions.requireNonNull(covMatrix, "prior.covariance"));
        this.params = Preconditions.requireNonNull(params, "prior.params");
        this.baseIndex = params.indexOf(pointId, 0);
    }

    @Override public double[] getObservedVector() { return observed.clone(); }
    @Override public SimpleMatrix getCovariance() { return covariance; }
    @Override public String[] involvedPointIds() { return new String[]{pointId}; }
    @Override public int dimension() { return 3; }

    @Override
    public double[] computeVector(double[] X) {
        return new double[]{
                X[baseIndex],
                X[baseIndex + 1],
                X[baseIndex + 2]
        };
    }

    @Override
    public void jacobian(double[] X, SimpleMatrix J) {
        J.zero();
        J.set(0, baseIndex,     1.0);
        J.set(1, baseIndex + 1, 1.0);
        J.set(2, baseIndex + 2, 1.0);
    }

    @Override
    public String toString() {
        return "PriorCoord{" + pointId
                + ", x=" + observed[0]
                + ", y=" + observed[1]
                + ", z=" + observed[2] + "}";
    }
}