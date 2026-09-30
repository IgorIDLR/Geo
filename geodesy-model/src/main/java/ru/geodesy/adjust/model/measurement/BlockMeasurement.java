package ru.geodesy.adjust.model.measurement;

import org.ejml.simple.SimpleMatrix;

/**
 * Векторное измерение с полной ковариационной матрицей.
 * <p>
 * Используется для GNSS-векторов (3×3), априорных координат
 * с корреляциями и других многомерных измерений.
 */
public interface BlockMeasurement {

    /** Вектор измеренных значений. */
    double[] getObservedVector();

    /** Ковариационная матрица размера m×m. */
    SimpleMatrix getCovariance();

    /** Вычисляет вектор значений по параметрам X. */
    double[] computeVector(double[] X);

    /** Заполняет матрицу Якоби J размера m×n. */
    void jacobian(double[] X, SimpleMatrix J);

    /** Идентификаторы точек. */
    String[] involvedPointIds();

    /** Размерность вектора. */
    default int dimension() {
        return getObservedVector().length;
    }
}