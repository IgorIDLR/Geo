package ru.geodesy.adjust.deformation.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.deformation.api.DeformationException;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.*;

/**
 * Pseudo-Epoch Approach — прямое сравнение координат между двумя эпохами.
 * <p>
 * Решает МНК отдельно для каждой эпохи, затем вычисляет разности координат.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PseudoEpochEstimator {

    private final DeformationProperties props;

    /**
     * Вычисляет смещения между двумя эпохами.
     *
     * @param net1          сеть первой эпохи
     * @param net2          сеть второй эпохи
     * @param commonPoints  список общих точек
     * @return карта: pointId → [dx, dy, dz]
     */
    public Map<String, double[]> estimate(Network net1, Network net2,
                                          List<String> commonPoints) {
        double[] X1 = solve(net1);
        double[] X2 = solve(net2);

        ParameterSet params = ParameterSet.fromPoints(net1.points());
        Map<String, double[]> result = new LinkedHashMap<>();

        for (String pid : commonPoints) {
            int b = params.indexOf(pid, 0);
            double dx = X2[b] - X1[b];
            double dy = X2[b + 1] - X1[b + 1];
            double dz = X2[b + 2] - X1[b + 2];
            result.put(pid, new double[]{dx, dy, dz});

            double mag = Math.sqrt(dx * dx + dy * dy + dz * dz);
            log.debug("Pseudo-epoch: точка {} смещение = {:.4f} м", pid, mag);
        }
        return result;
    }

    private double[] solve(Network network) {
        ParameterSet params = ParameterSet.fromPoints(network.points());
        double[] X = params.getInitial();
        int k = params.size();

        for (int iter = 0; iter < props.getMaxIterations(); iter++) {
            SimpleMatrix N = new SimpleMatrix(k, k);
            SimpleMatrix b = new SimpleMatrix(k, 1);

            for (Measurement m : network.measurements()) {
                double[] a = new double[k];
                m.derivatives(X, a);
                double l = m.getObserved() - m.computeValue(X);
                double w = 1.0 / m.getVariance();

                for (int r = 0; r < k; r++) {
                    if (Math.abs(a[r]) < 1e-15) continue;
                    b.set(r, 0, b.get(r, 0) + w * a[r] * l);
                    for (int c = 0; c < k; c++) {
                        if (Math.abs(a[c]) < 1e-15) continue;
                        N.set(r, c, N.get(r, c) + w * a[r] * a[c]);
                    }
                }
            }

            // Регуляризация
            for (int i = 0; i < k; i++) {
                N.set(i, i, N.get(i, i) + 1e-10);
            }

            SimpleMatrix delta = N.solve(b);
            double maxD = 0;
            for (int i = 0; i < k; i++) {
                double d = delta.get(i, 0);
                X[i] += d;
                maxD = Math.max(maxD, Math.abs(d));
            }
            if (maxD < props.getConvergenceEps()) break;
        }
        return X;
    }
}