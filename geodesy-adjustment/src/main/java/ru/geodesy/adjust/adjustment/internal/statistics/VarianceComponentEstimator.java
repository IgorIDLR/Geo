package ru.geodesy.adjust.adjustment.internal.statistics;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.*;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.*;

/**
 * Оценка компонент дисперсии методом Гельмгерта.
 * σ²_g = vᵀ P_g v / trace(Qv · P_g)
 * с принудительным PSD-контролем.
 */
@Slf4j
@Component
public class VarianceComponentEstimator {

    private static final int MAX_ITER = 30;
    private static final double TOL = 1e-6;
    private static final double NEGATIVE_LIMIT = 1e-10;

    public record VceResult(Map<String, Double> sigma2,
                            double[] refinedVariances,
                            int iterations,
                            boolean converged) {}

    public VceResult estimate(Network network, ParameterSet params, double[] X,
                              List<String> groupOf, double[] initialVar) {
        int n = network.measurements().size();
        int k = params.size();

        Set<String> groups = new LinkedHashSet<>(groupOf);
        if (groups.size() < 2) {
            return new VceResult(Map.of(), initialVar.clone(), 0, true);
        }

        double[] variances = initialVar.clone();
        Map<String, Double> sigma2 = new LinkedHashMap<>();
        for (String g : groups) sigma2.put(g, 1.0);

        int iter = 0;
        boolean converged = false;

        for (; iter < MAX_ITER; iter++) {
            RealMatrix A = new Array2DRowRealMatrix(n, k);
            RealMatrix P = new Array2DRowRealMatrix(n, n);
            RealVector y = new ArrayRealVector(n);

            for (int i = 0; i < n; i++) {
                Measurement m = network.measurements().get(i);
                double[] deriv = new double[k];
                m.derivatives(X, deriv);
                for (int j = 0; j < k; j++) A.setEntry(i, j, deriv[j]);
                P.setEntry(i, i, 1.0 / variances[i]);
                y.setEntry(i, m.getObserved() - m.computeValue(X));
            }

            RealMatrix N = A.transpose().multiply(P).multiply(A);
            RealVector rhs = A.transpose().multiply(P).operate(y);

            DecompositionSolver solver = new LUDecomposition(N).getSolver();
            if (!solver.isNonSingular()) {
                log.warn("VCE: N вырождена на итерации {}", iter);
                break;
            }
            RealVector delta = solver.solve(rhs);
            for (int j = 0; j < k; j++) X[j] += delta.getEntry(j);

            RealMatrix Qx = new SingularValueDecomposition(N)
                    .getSolver().getInverse();
            RealMatrix Pinv = new DiagonalMatrix(variances.clone());

            RealMatrix Qv = Pinv.subtract(A.multiply(Qx).multiply(A.transpose()));

            for (int i = 0; i < n; i++) {
                if (Qv.getEntry(i, i) < 0) {
                    Qv.setEntry(i, i, 0);
                }
            }

            Map<String, Double> newSigma2 = new LinkedHashMap<>();
            boolean allPositive = true;

            for (String g : groups) {
                double vPv = 0;
                double traceQvPg = 0;

                for (int i = 0; i < n; i++) {
                    if (!groupOf.get(i).equals(g)) continue;
                    double p = P.getEntry(i, i);
                    double vi = y.getEntry(i);

                    vPv += vi * vi * p;
                    for (int j = 0; j < n; j++) {
                        if (!groupOf.get(j).equals(g)) continue;
                        traceQvPg += Qv.getEntry(i, j) * P.getEntry(j, j);
                    }
                }

                double s2 = traceQvPg > 1e-15
                        ? vPv / traceQvPg
                        : sigma2.get(g);
                if (s2 < NEGATIVE_LIMIT) {
                    allPositive = false;
                    s2 = NEGATIVE_LIMIT;
                }
                newSigma2.put(g, s2);
            }

            double maxChange = 0;
            for (String g : groups) {
                maxChange = Math.max(maxChange,
                        Math.abs(newSigma2.get(g) - sigma2.get(g)));
                sigma2.put(g, newSigma2.get(g));
            }

            for (int i = 0; i < n; i++) {
                variances[i] = initialVar[i]
                        * sigma2.getOrDefault(groupOf.get(i), 1.0);
            }

            log.info("VCE итерация {}: maxChange = {:.3e}", iter + 1, maxChange);
            if (maxChange < TOL && allPositive) {
                converged = true;
                break;
            }
        }
        return new VceResult(sigma2, variances, iter + 1, converged);
    }
}