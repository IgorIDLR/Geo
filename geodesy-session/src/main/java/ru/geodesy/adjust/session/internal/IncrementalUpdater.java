package ru.geodesy.adjust.session.internal;

import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.adjust.model.measurement.SparseMeasurement;

/**
 * Инкрементальное обновление Q по формуле Шермана-Моррисона.
 * <p>
 * Для добавления измерения:
 * <pre>
 *   Q_new = Q - (Q·aᵀ·a·Q) / (1/p + a·Q·aᵀ)
 * </pre>
 * Для удаления:
 * <pre>
 *   Q_new = Q + (Q·aᵀ·a·Q) / (1/p - a·Q·aᵀ)
 * </pre>
 * Сложность O(n²) вместо O(n³) при полном обращении [citation:8].
 */
@Slf4j
class IncrementalUpdater {

    private static final double EPS = 1e-15;

    /**
     * Добавление измерения.
     */
    static boolean add(SimpleMatrix Q, SimpleMatrix b, double[] X,
                       Measurement m, ParameterSet params) {
        int n = params.size();
        double[] a = new double[n];
        m.derivatives(X, a);

        SimpleMatrix av = new SimpleMatrix(n, 1);
        for (int i = 0; i < n; i++) av.set(i, 0, a[i]);

        double p = 1.0 / m.getVariance();
        SimpleMatrix Qat = Q.mult(av.transpose());
        double aQa = av.transpose().mult(Qat).get(0, 0);
        double denom = 1.0 / p + aQa;

        if (Math.abs(denom) < EPS) {
            log.warn("Инкрементальное добавление: вырожденный знаменатель");
            return false;
        }

        SimpleMatrix correction = Qat.mult(av).mult(Q).divide(denom);
        Q.set(Q.minus(correction));

        double l = m.getObserved() - m.computeValue(X);
        for (int i = 0; i < n; i++) {
            b.set(i, 0, b.get(i, 0) + p * a[i] * l);
        }
        return true;
    }

    /**
     * Удаление измерения. Требует, чтобы измерение было избыточным.
     */
    static boolean remove(SimpleMatrix Q, SimpleMatrix b, double[] X,
                          Measurement m, ParameterSet params) {
        int n = params.size();
        double[] a = new double[n];
        m.derivatives(X, a);

        SimpleMatrix av = new SimpleMatrix(n, 1);
        for (int i = 0; i < n; i++) av.set(i, 0, a[i]);

        double p = 1.0 / m.getVariance();
        SimpleMatrix Qat = Q.mult(av.transpose());
        double aQa = av.transpose().mult(Qat).get(0, 0);
        double denom = 1.0 / p - aQa;

        if (denom <= EPS) {
            log.warn("Невозможно удалить необходимое измерение (redundancy ≤ 0)");
            return false;
        }

        SimpleMatrix correction = Qat.mult(av).mult(Q).divide(denom);
        Q.set(Q.plus(correction));

        double l = m.getObserved() - m.computeValue(X);
        for (int i = 0; i < n; i++) {
            b.set(i, 0, b.get(i, 0) - p * a[i] * l);
        }
        return true;
    }

    /**
     * Изменение = удаление + добавление.
     */
    static boolean update(SimpleMatrix Q, SimpleMatrix b, double[] X,
                          Measurement oldM, Measurement newM, ParameterSet params) {
        if (!remove(Q, b, X, oldM, params)) return false;
        return add(Q, b, X, newM, params);
    }
}