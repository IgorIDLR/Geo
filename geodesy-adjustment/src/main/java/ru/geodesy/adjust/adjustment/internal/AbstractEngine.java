package ru.geodesy.adjust.adjustment.internal;

import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.adjustment.api.AdjustmentResult;
import ru.geodesy.adjust.adjustment.internal.assembler.NormalEquationAssembler;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Базовый класс движков: общие методы.
 */
public abstract class AbstractEngine {

    protected final NormalEquationAssembler assembler;

    protected AbstractEngine(NormalEquationAssembler assembler) {
        this.assembler = assembler;
    }

    protected double[] initParameters(Network network) {
        return ParameterSet.fromPoints(network.points()).getInitial();
    }

    protected double computeReferenceError(Network network, double[] X,
                                           double[] variances) {
        double vTPv = 0.0;
        int idx = 0;
        for (Measurement m : network.measurements()) {
            double v = m.getObserved() - m.computeValue(X);
            vTPv += (v * v) / variances[idx++];
        }
        int n = X.length;
        int defRank = network.networkType().totalDefectRank();
        int dof = network.measurements().size() - n + defRank;
        return dof > 0 ? Math.sqrt(vTPv / dof) : 0.0;
    }

    protected AdjustmentResult buildResult(Network network, double[] X,
                                           SimpleMatrix Q,
                                           double refErr, int iter,
                                           boolean converged,
                                           List<String> blunders,
                                           String method,
                                           long elapsed) {
        ParameterSet params = ParameterSet.fromPoints(network.points());

        Map<String, double[]> coords = new LinkedHashMap<>();
        for (Point p : network.points()) {
            int base = params.indexOf(p.id(), 0);
            coords.put(p.id(), new double[]{
                    X[base], X[base + 1], X[base + 2]
            });
        }

        Map<String, double[][]> covars = new LinkedHashMap<>();
        if (Q != null) {
            for (Point p : network.points()) {
                int base = params.indexOf(p.id(), 0);
                double[][] blk = new double[3][3];
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        blk[r][c] = Q.get(base + r, base + c);
                    }
                }
                covars.put(p.id(), blk);
            }
        }

        return new AdjustmentResult(coords, covars, refErr, iter, converged,
                blunders, method, elapsed);
    }
}