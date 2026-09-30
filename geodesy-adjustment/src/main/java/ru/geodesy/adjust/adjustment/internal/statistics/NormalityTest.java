package ru.geodesy.adjust.adjustment.internal.statistics;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.measurement.Measurement;

/**
 * Тест Жарка-Бера на нормальность невязок уравнивания.
 * <p>
 * <b>Формула:</b>
 * <pre>
 *   JB = n/6 · (S² + (K − 3)²/4) ~ χ²(2)
 * </pre>
 * где S — выборочная асимметрия, K — выборочный эксцесс.
 * <p>
 * <b>Важно:</b> формула JB выведена для <b>выборочных</b> (biased) моментов,
 * поэтому используется {@code biasCorrected = false}.
 * При использовании несмещённых моментов результаты систематически смещены.
 * <p>
 * <b>Ограничения метода:</b>
 * <ul>
 *   <li>Низкая мощность при n < 100. Для малых выборок предпочтительнее
 *       тест Шапиро-Уилка.</li>
 *   <li>Предполагает <b>независимость</b> наблюдений. В МНК невязки
 *       коррелированы через матрицу Q_v — это может давать ложные
 *       отклонения нормальности.</li>
 *   <li>Чувствителен к выбросам: одна аномальная невязка может
 *       отвергнуть нормальность всей выборки.</li>
 * </ul>
 *
 * @implNote для устойчивой проверки нормальности в геодезии чаще
 *           применяют χ²-тест апостериорной σ₀ — он менее чувствителен
 *           к коррелированности остатков.
 */
@Slf4j
public final class NormalityTest {

    /** Минимальный размер выборки для применения JB-теста. */
    private static final int MIN_SAMPLE_SIZE = 20;

    /** Рекомендуемый минимальный размер для надёжного JB-теста. */
    private static final int RECOMMENDED_SAMPLE_SIZE = 100;

    /** Порог дисперсии — при меньшем считаем выборку вырожденной. */
    private static final double VARIANCE_EPSILON = 1e-15;

    private final double[] residuals;
    private final double variance;
    private final double skewness;
    private final double kurtosis;
    private final boolean degenerate;

    /**
     * Создаёт тест для невязок сети при данном векторе параметров.
     *
     * @param network сеть с измерениями
     * @param X       уравненный вектор параметров
     */
    public NormalityTest(Network network, double[] X) {
        var meas = network.measurements();
        this.residuals = new double[meas.size()];
        for (int i = 0; i < meas.size(); i++) {
            Measurement m = meas.get(i);
            residuals[i] = m.getObserved() - m.computeValue(X);
        }

        this.variance = computeVariance(residuals);

        if (variance < VARIANCE_EPSILON) {
            // Вырожденный случай: все невязки одинаковы
            this.degenerate = true;
            this.skewness = 0.0;
            this.kurtosis = 3.0;
            log.debug("NormalityTest: вырожденная выборка (variance ≈ 0)");
        } else {
            this.degenerate = false;
            this.skewness = new Skewness().evaluate(residuals);

            // ВАЖНО: biased = false означает biasCorrected = false,
            // т.е. выборочный (biased) эксцесс — это то, что нужно для JB.
            this.kurtosis = new Kurtosis().evaluate(residuals);
        }
    }

    // ============================================================
    // Публичные метрики
    // ============================================================

    /** Размер выборки. */
    public int sampleSize() {
        return residuals.length;
    }

    /** Выборочный коэффициент асимметрии (biased). */
    public double skewness() {
        return skewness;
    }

    /** Выборочный эксцесс (biased). Для нормального распределения ≈ 3. */
    public double kurtosis() {
        return kurtosis;
    }

    /** Дисперсия невязок. */
    public double variance() {
        return variance;
    }

    /** Стандартное отклонение невязок. */
    public double standardDeviation() {
        return Math.sqrt(variance);
    }

    // ============================================================
    // Статистика и проверка
    // ============================================================

    /**
     * Статистика Жарка-Бера.
     * <p>
     * При {@code degenerate = true} возвращает 0.0 (нет отклонения
     * от нормальности).
     */
    public double jarqueBera() {
        if (degenerate) return 0.0;
        int n = residuals.length;
        double S = skewness;
        double K = kurtosis;
        return n / 6.0 * (S * S + (K - 3) * (K - 3) / 4.0);
    }

    /**
     * Проверка гипотезы нормальности.
     *
     * @param alpha уровень значимости (0 &lt; α &lt; 1)
     * @return {@code true}, если нулевая гипотеза нормальности
     *         <b>не отвергается</b> на уровне α
     */
    public boolean isNormal(double alpha) {
        if (alpha <= 0 || alpha >= 1) {
            throw new IllegalArgumentException(
                    "Уровень значимости α должен быть в (0, 1): " + alpha);
        }

        // Выборка вырождена — нет данных для отвержения
        if (degenerate) return true;

        // Слишком малая выборка — тест ненадёжен
        int n = residuals.length;
        if (n < MIN_SAMPLE_SIZE) {
            log.warn("JB-тест неприменим при n = {} < {} — принимаем "
                    + "нормальность по умолчанию", n, MIN_SAMPLE_SIZE);
            return true;
        }

        if (n < RECOMMENDED_SAMPLE_SIZE) {
            log.info("JB-тест имеет низкую мощность при n = {} < {}. "
                    + "Рассмотрите тест Шапиро-Уилка", n, RECOMMENDED_SAMPLE_SIZE);
        }

        // Точное критическое значение χ²(2) через Commons Math
        ChiSquaredDistribution chi2 = new ChiSquaredDistribution(2.0);
        double criticalValue = chi2.inverseCumulativeProbability(1 - alpha);

        double jb = jarqueBera();
        boolean normal = jb < criticalValue;

        log.debug("JB = {:.4f}, χ²(2, α={}) = {:.4f}, normal = {}",
                jb, alpha, criticalValue, normal);

        return normal;
    }

    /**
     * Возвращает p-value для статистики JB.
     * <p>
     * p-value &gt; α — нет оснований отвергать нормальность.
     */
    public double pValue() {
        if (degenerate) return 1.0;
        ChiSquaredDistribution chi2 = new ChiSquaredDistribution(2.0);
        return 1.0 - chi2.cumulativeProbability(jarqueBera());
    }

    /**
     * Оценка уровня значимости (α) для текущего JB.
     * Это "пороговое" α, при котором гипотеза отвергается.
     */
    public String describe() {
        if (degenerate) {
            return "Невязки вырождены (variance ≈ 0)";
        }
        int n = residuals.length;
        return String.format(
                "JB-тест: n = %d, S = %.4f, K = %.4f, JB = %.4f, p-value = %.4f%s",
                n, skewness, kurtosis, jarqueBera(), pValue(),
                n < RECOMMENDED_SAMPLE_SIZE ? " (малая выборка)" : "");
    }

    // ============================================================
    // Утилиты
    // ============================================================

    private static double computeVariance(double[] values) {
        if (values.length < 2) return 0.0;
        double sum = 0;
        for (double v : values) sum += v;
        double mean = sum / values.length;

        double sumSq = 0;
        for (double v : values) {
            double d = v - mean;
            sumSq += d * d;
        }
        return sumSq / (values.length - 1);
    }
}