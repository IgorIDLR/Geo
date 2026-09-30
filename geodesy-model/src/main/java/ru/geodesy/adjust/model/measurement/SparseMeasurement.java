package ru.geodesy.adjust.model.measurement;

/**
 * Расширение {@link Measurement}, оптимизированное для разреженной
 * сборки нормальных уравнений.
 * <p>
 * Позволяет измерению вернуть только ненулевые производные, что
 * снижает сложность сборки с O(n²) до O(k²), где k — число
 * задействованных параметров (обычно 4–6).
 */
public interface SparseMeasurement extends Measurement {

    /**
     * Заполняет массивы индексов и значений ненулевых производных.
     *
     * @param X       вектор параметров
     * @param indices массив для индексов (минимум {@link #maxSparseSize()})
     * @param values  массив для значений
     * @return фактическое число заполненных элементов
     */
    int sparseDerivatives(double[] X, int[] indices, double[] values);

    /** Максимальное число ненулевых производных. */
    int maxSparseSize();
}