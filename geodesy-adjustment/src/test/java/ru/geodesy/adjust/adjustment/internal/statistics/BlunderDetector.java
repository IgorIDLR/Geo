package ru.geodesy.adjust.adjustment.internal.statistics;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.ArrayList;
import java.util.List;

/**
 * Обнаружение грубых ошибок по критерию Баарды с поправкой Бонферрони.
 * <p>
 * <b>Ключевая идея:</b> проверяемыми (testable) являются те измерения,
 * у которых избыточность положительна:
 * <pre>
 *   r_i = σ²_i − a_i · Q · a_i^T > 0
 * </pre>
 * где σ²_i — априорная дисперсия измерения, Q — ковариационная матрица
 * параметров, a_i — вектор производных.
 * <p>
 * Для необходимых измерений (r_i = 0) невязка тождественно равна нулю,
 * и тест принципиально невозможен.
 */
@Slf4j
@Component
public class BlunderDetector {

    /** Порог избыточности — ниже считаем измерение необходимым. */
    private static final double REDUNDANCY_EPSILON = 1e-12;

    public record Blunder(int measurementIndex,
                          double wStatistic,
                          double residual,
                          double minDetectableError) {}

    public List<Blunder> detect(Network network, ParameterSet params,
                                double[] X, SimpleMatrix Q, double sigma0,
                                double alpha0, double beta0) {
        List<Measurement> all = network.measurements();
        int n = all.size();
        int k = params.size();

        // ============================================================
        // Первый проход: определяем проверяемые измерения
        // ============================================================
        List<Integer> testable = new ArrayList<>();
        double[] qvValues = new double[n];
        double[] residuals = new double[n];

        for (int i = 0; i < n; i++) {
            Measurement m = all.get(i);

            // Вектор производных
            double[] a = new double[k];
            m.derivatives(X, a);
            SimpleMatrix ai = new SimpleMatrix(k, 1);
            for (int j = 0; j < k; j++) {
                ai.set(j, 0, a[j]);
            }

            // Избыточность: r_i = σ²_i − a_i · Q · a_i^T
            double aQa = ai.transpose().mult(Q).mult(ai).get(0, 0);
            double qv = m.getVariance() - aQa;

            qvValues[i] = qv;
            residuals[i] = m.getObserved() - m.computeValue(X);

            if (qv > REDUNDANCY_EPSILON) {
                testable.add(i);
            }
        }

        int rHypotheses = testable.size();

        if (rHypotheses == 0) {
            log.info("Все измерения необходимые (нет избыточности) — "
                    + "обнаружение выбросов невозможно");
            return List.of();
        }

        log.debug("BlunderDetector: {} из {} измерений проверяемы",
                rHypotheses, n);

        // ============================================================
        // Поправка Бонферрони по числу проверяемых измерений
        // ============================================================
        double alphaBonf = alpha0 / rHypotheses;
        NormalDistribution normal = new NormalDistribution();
        double tAlpha = normal.inverseCumulativeProbability(1 - alphaBonf / 2);
        double tBeta = normal.inverseCumulativeProbability(beta0);
        double lambda0 = Math.pow(tAlpha + tBeta, 2);

        log.debug("α_bonf = {:.3e}, t_α = {:.3f}, t_β = {:.3f}, λ₀ = {:.3f}",
                alphaBonf, tAlpha, tBeta, lambda0);

        // ============================================================
        // Второй проход: проверка каждого проверяемого измерения
        // ============================================================
        List<Blunder> blunders = new ArrayList<>();

        for (int i : testable) {
            double qv = qvValues[i];
            double v = residuals[i];

            // Стандартизованная невязка
            double w = Math.abs(v) / (sigma0 * Math.sqrt(qv));

            if (w * w > lambda0) {
                double minDetect = sigma0 * Math.sqrt(qv * lambda0);
                blunders.add(new Blunder(i, w, v, minDetect));
                log.warn("Выброс: измерение {} (w = {:.3f}, v = {:.6f}, "
                        + "min detect = {:.6f})", i, w, v, minDetect);
            }
        }

        log.info("Обнаружено {} выбросов из {} проверяемых измерений",
                blunders.size(), rHypotheses);

        return blunders;
    }
}