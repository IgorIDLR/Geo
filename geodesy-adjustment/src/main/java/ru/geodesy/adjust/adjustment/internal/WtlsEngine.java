package ru.geodesy.adjust.adjustment.internal;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.*;
import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.adjustment.api.AdjustmentEngine;
import ru.geodesy.adjust.adjustment.api.AdjustmentOptions;
import ru.geodesy.adjust.adjustment.api.AdjustmentResult;
import ru.geodesy.adjust.adjustment.internal.assembler.NormalEquationAssembler;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.List;

/**
 * WTLS через SVD расширенной матрицы [A | y].
 * Правильный выбор наименьшего сингулярного вектора.
 */
@Slf4j
public class WtlsEngine extends AbstractEngine {

    private static final int MAX_ITER = 50;

    public WtlsEngine(NormalEquationAssembler assembler) {
        super(assembler);
    }

    public AdjustmentEngine asEngine() {
        return new Adapter(this);
    }

    public AdjustmentResult adjustInternal(Network network, double[] variances,
                                           AdjustmentOptions options) {
        long start = System.currentTimeMillis();
        ParameterSet params = ParameterSet.fromPoints(network.points());
        double[] X = params.getInitial();
        int n = network.measurements().size();
        int k = params.size();

        int effectiveMax = Math.min(options.maxIterations(), MAX_ITER);
        int iter = 0;
        boolean converged = false;

        for (; iter < effectiveMax; iter++) {
            RealMatrix A = new Array2DRowRealMatrix(n, k);
            RealVector y = new ArrayRealVector(n);

            for (int i = 0; i < n; i++) {
                Measurement m = network.measurements().get(i);
                double[] deriv = new double[k];
                m.derivatives(X, deriv);
                double w = 1.0 / Math.sqrt(variances[i]);
                for (int j = 0; j < k; j++) A.setEntry(i, j, deriv[j] * w);
                y.setEntry(i, (m.getObserved() - m.computeValue(X)) * w);
            }

            RealMatrix C = new Array2DRowRealMatrix(n, k + 1);
            C.setSubMatrix(A.getData(), 0, 0);
            C.setColumnVector(k, y);

            SingularValueDecomposition svd = new SingularValueDecomposition(C);
            double[] singularValues = svd.getSingularValues();

            int minIdx = 0;
            double minSingular = Double.MAX_VALUE;
            for (int i = 0; i < singularValues.length; i++) {
                if (singularValues[i] < minSingular) {
                    minSingular = singularValues[i];
                    minIdx = i;
                }
            }

            RealMatrix V = svd.getV();
            RealVector vMin = V.getColumnVector(minIdx);

            double vk = vMin.getEntry(k);
            if (Math.abs(vk) < 1e-12) {
                log.warn("WTLS: v[k] ≈ 0, применяю LS-fallback");
                return fallbackLS(network, params, X, variances, options, start);
            }

            double[] delta = new double[k];
            double maxDelta = 0;
            for (int j = 0; j < k; j++) {
                delta[j] = -vMin.getEntry(j) / vk;
                X[j] += delta[j];
                maxDelta = Math.max(maxDelta, Math.abs(delta[j]));
            }

            log.debug("WTLS итерация {}: maxDelta = {:.3e}, min σ = {}",
                    iter + 1, maxDelta, minSingular);

            if (maxDelta < options.convergenceEps()) {
                converged = true;
                iter++;
                break;
            }
        }

        SimpleMatrix Q = computeFinalCovariance(network, params, X, variances);
        double refErr = computeReferenceError(network, X, variances);
        return buildResult(network, X, Q, refErr, iter, converged,
                List.of(), "WTLS",
                System.currentTimeMillis() - start);
    }

    private AdjustmentResult fallbackLS(Network network, ParameterSet params, double[] X,
                                        double[] variances, AdjustmentOptions options,
                                        long start) {
        var eq = assembler.assemble(network, params, X);
        SimpleMatrix delta = eq.N().solve(eq.b());
        for (int j = 0; j < X.length; j++) X[j] += delta.get(j, 0);

        SimpleMatrix Q = eq.N().invert();
        double refErr = computeReferenceError(network, X, variances);
        return buildResult(network, X, Q, refErr, 1, true,
                List.of(), "WTLS-FALLBACK",
                System.currentTimeMillis() - start);
    }

    private SimpleMatrix computeFinalCovariance(Network network, ParameterSet params,
                                                double[] X, double[] variances) {
        var eq = assembler.assemble(network, params, X);
        return eq.N().invert();
    }

    private static final class Adapter implements AdjustmentEngine {
        private final WtlsEngine delegate;

        Adapter(WtlsEngine delegate) {
            this.delegate = delegate;
        }

        @Override
        public AdjustmentResult adjust(Network network, double[] initialVar,
                                       AdjustmentOptions options) {
            return delegate.adjustInternal(network, initialVar, options);
        }

        @Override
        public String methodName() {
            return "WTLS";
        }
    }
}