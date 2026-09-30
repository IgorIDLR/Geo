package ru.geodesy.adjust.adjustment.internal;

import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.adjustment.api.AdjustmentEngine;
import ru.geodesy.adjust.adjustment.api.AdjustmentOptions;
import ru.geodesy.adjust.adjustment.api.AdjustmentResult;
import ru.geodesy.adjust.adjustment.internal.assembler.NormalEquationAssembler;
import ru.geodesy.adjust.adjustment.internal.constraints.FreeNetworkConstraints;
import ru.geodesy.adjust.adjustment.internal.robust.WeightFunction;
import ru.geodesy.adjust.adjustment.internal.update.UdUpdater;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.ArrayList;

/**
 * Рекуррентный способ уравнивания с UD-обновлением Q.
 */
@Slf4j
@Component
@Scope("prototype")
public class RecurrentEngine extends AbstractEngine {

    private WeightFunction weightFunction;

    public RecurrentEngine(NormalEquationAssembler assembler) {
        super(assembler);
    }

    public RecurrentEngine withWeightFunction(WeightFunction wf) {
        this.weightFunction = wf;
        return this;
    }

    public AdjustmentEngine asEngine() {
        return new Adapter(this);
    }

    public AdjustmentResult adjustInternal(Network network, double[] variances,
                                           AdjustmentOptions options) {
        long start = System.currentTimeMillis();

        ParameterSet params = ParameterSet.fromPoints(network.points());
        double[] X = params.getInitial();
        int n = X.length;
        boolean isFree = network.networkType().isFree();

        SimpleMatrix Q = SimpleMatrix.identity(n).scale(1e8);
        var meas = network.measurements();

        double[] curWeights = new double[meas.size()];
        for (int i = 0; i < meas.size(); i++) {
            curWeights[i] = 1.0 / variances[i];
        }

        boolean converged = false;
        int iter = 0;

        for (; iter < options.maxIterations(); iter++) {
            double maxDelta = 0.0;

            for (int i = 0; i < meas.size(); i++) {
                Measurement m = meas.get(i);
                double weight = curWeights[i];
                double obs = m.getObserved();
                double calc = m.computeValue(X);
                double L = calc - obs;

                double[] deriv = new double[n];
                m.derivatives(X, deriv);
                SimpleMatrix a = new SimpleMatrix(n, 1);
                for (int j = 0; j < n; j++) a.set(j, 0, deriv[j]);

                SimpleMatrix aq = Q.mult(a);
                double N = 1.0 / weight + a.transpose().mult(aq).get(0, 0);
                SimpleMatrix K = aq.divide(N);

                for (int j = 0; j < n; j++) {
                    double dx = -K.get(j, 0) * L;
                    X[j] += dx;
                    maxDelta = Math.max(maxDelta, Math.abs(dx));
                }

                Q = UdUpdater.update(Q, aq, N);
                Q = Q.plus(Q.transpose()).scale(0.5);

                if (weightFunction != null) {
                    double r = obs - calc;
                    double sigma = Math.sqrt(variances[i]);
                    double factor = weightFunction.compute(r, sigma);
                    curWeights[i] = (1.0 / variances[i]) * Math.max(factor, 1e-12);
                }
            }

            log.debug("Recurrent iter {}: maxDelta = {}", iter + 1, maxDelta);

            if (maxDelta < options.convergenceEps()) {
                converged = true;
                iter++;
                break;
            }
        }

        if (!converged) {
            log.warn("Рекуррентное уравнивание не сошлось за {} итераций",
                    options.maxIterations());
        }

        SimpleMatrix Qfinal = Q;
        if (isFree) {
            SimpleMatrix Nfull = assembler.assemble(network, params, X).N();
            Nfull = FreeNetworkConstraints.apply(Nfull, network);
            Qfinal = Nfull.invert();
        }

        double refErr = computeReferenceError(network, X, variances);
        return buildResult(network, X, Qfinal, refErr, iter, converged,
                new ArrayList<>(), "RECURRENT",
                System.currentTimeMillis() - start);
    }

    private static final class Adapter implements AdjustmentEngine {
        private final RecurrentEngine delegate;

        Adapter(RecurrentEngine delegate) {
            this.delegate = delegate;
        }

        @Override
        public AdjustmentResult adjust(Network network, double[] initialVar,
                                       AdjustmentOptions options) {
            return delegate.adjustInternal(network, initialVar, options);
        }

        @Override
        public String methodName() {
            return "RECURRENT";
        }
    }
}