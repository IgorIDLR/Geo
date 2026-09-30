package ru.geodesy.adjust.deformation.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.deformation.api.PointDisplacement;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.common.MathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Squared Msplit Estimation — итеративное разделение наблюдений
 * на две гипотезы: "устойчивая" (S) и "неустойчивая" (U).
 * <p>
 * На каждой EM-итерации:
 * <ol>
 *   <li>Решаем взвешенный МНК для гипотезы S с весами = prob_i</li>
 *   <li>Решаем взвешенный МНК для гипотезы U с весами = 1 − prob_i</li>
 *   <li>Пересчитываем вероятности prob_i через отношения правдоподобий</li>
 * </ol>
 * Сходимость — по изменению вектора вероятностей.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SquaredMsplitEstimator {

    private final DeformationProperties props;

    public record MsplitResult(
            double[] XStable,
            double[] XUnstable,
            double[] probabilities,
            int iterations,
            boolean converged
    ) {}

    /**
     * Запускает итеративную схему Msplit.
     *
     * @param network   сеть с измерениями
     * @param params    множество параметров
     * @param initialX  начальное приближение
     * @param variances дисперсии измерений
     */
    public MsplitResult estimate(Network network, ParameterSet params,
                                 double[] initialX, double[] variances) {
        int n = network.measurements().size();
        int k = params.size();

        double[] Xs = initialX.clone();
        double[] Xu = initialX.clone();
        double[] probs = new double[n];
        Arrays.fill(probs, 0.5);

        int iter = 0;
        boolean converged = false;
        double prevMaxChange = Double.MAX_VALUE;

        for (; iter < props.getMaxIterations(); iter++) {
            // 1. Взвешенный МНК для S
            double[] XsNew = solveWeighted(network, params, Xs, variances, probs, true);

            // 2. Взвешенный МНК для U
            double[] XuNew = solveWeighted(network, params, Xu, variances, probs, false);

            // 3. EM-шаг: пересчёт вероятностей
            double maxChange = 0;
            double entropySum = 0;

            for (int i = 0; i < n; i++) {
                Measurement m = network.measurements().get(i);
                double rS = m.getObserved() - m.computeValue(XsNew);
                double rU = m.getObserved() - m.computeValue(XuNew);

                double logLS = -rS * rS / (2 * variances[i]);
                double logLU = -rU * rU / (2 * variances[i]);

                // Логарифмическая стабилизация
                double maxLog = Math.max(logLS, logLU);
                double pS = Math.exp(logLS - maxLog);
                double pU = Math.exp(logLU - maxLog);
                double pNew = pS / (pS + pU + 1e-15);

                maxChange = Math.max(maxChange, Math.abs(pNew - probs[i]));
                probs[i] = pNew;

                double entropy = -pNew * Math.log(pNew + 1e-15)
                        - (1 - pNew) * Math.log(1 - pNew + 1e-15);
                entropySum += entropy;
            }

            Xs = XsNew;
            Xu = XuNew;

            log.debug("Msplit iter {}: maxChange = {}, entropy = {}",
                    iter + 1, maxChange, entropySum / n);

            // Ранний останов при стагнации
            if (maxChange < props.getConvergenceEps()) {
                converged = true;
                iter++;
                break;
            }
            if (maxChange > prevMaxChange * 0.99 && iter > 10) {
                log.info("Msplit: стагнация на итерации {}", iter + 1);
                iter++;
                break;
            }
            prevMaxChange = maxChange;
        }

        return new MsplitResult(Xs, Xu, probs, iter, converged);
    }

    /**
     * Взвешенный МНК для одной гипотезы.
     */
    private double[] solveWeighted(Network network, ParameterSet params,
                                   double[] X, double[] variances,
                                   double[] probs, boolean stableHypothesis) {
        int n = network.measurements().size();
        int k = params.size();
        SimpleMatrix N = new SimpleMatrix(k, k);
        SimpleMatrix b = new SimpleMatrix(k, 1);

        for (int i = 0; i < n; i++) {
            Measurement m = network.measurements().get(i);
            double w = (stableHypothesis ? probs[i] : 1 - probs[i]) / variances[i];
            if (w < 1e-15) continue;

            double[] a = new double[k];
            m.derivatives(X, a);
            double l = m.getObserved() - m.computeValue(X);

            for (int r = 0; r < k; r++) {
                if (Math.abs(a[r]) < 1e-15) continue;
                b.set(r, 0, b.get(r, 0) + w * a[r] * l);
                for (int c = 0; c < k; c++) {
                    if (Math.abs(a[c]) < 1e-15) continue;
                    N.set(r, c, N.get(r, c) + w * a[r] * a[c]);
                }
            }
        }

        // Регуляризация для устойчивости
        for (int i = 0; i < k; i++) {
            N.set(i, i, N.get(i, i) + 1e-10);
        }

        SimpleMatrix delta = N.solve(b);
        double[] Xnew = X.clone();
        for (int i = 0; i < k; i++) {
            Xnew[i] += delta.get(i, 0);
        }
        return Xnew;
    }

    /**
     * Преобразует результат Msplit в список смещений точек.
     */
    public List<PointDisplacement> toDisplacements(MsplitResult res,
                                                   ParameterSet params,
                                                   Network network,
                                                   double sigma0) {
        List<PointDisplacement> result = new ArrayList<>();
        for (Point p : network.points()) {
            if (p.fixed()) continue;
            int base = params.indexOf(p.id(), 0);

            double dx = res.XUnstable()[base] - res.XStable()[base];
            double dy = res.XUnstable()[base + 1] - res.XStable()[base + 1];
            double dz = res.XUnstable()[base + 2] - res.XStable()[base + 2];
            double magnitude = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double sigmaDx = sigma0 * 0.001;
            double sigmaDy = sigma0 * 0.001;
            double sigmaDz = sigma0 * 0.001;

            boolean significant = magnitude > props.getSignificanceThreshold();

            result.add(new PointDisplacement(
                    p.id(), dx, dy, dz,
                    sigmaDx, sigmaDy, sigmaDz,
                    magnitude, significant, 1 - props.getSignificanceLevel()
            ));
        }
        return result;
    }
}