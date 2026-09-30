package ru.geodesy.adjust.adjustment.internal;

import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.adjustment.api.AdjustmentEngine;
import ru.geodesy.adjust.adjustment.api.AdjustmentOptions;
import ru.geodesy.adjust.adjustment.api.AdjustmentResult;
import ru.geodesy.adjust.adjustment.internal.assembler.NormalEquationAssembler;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.LevelingMeasurement;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.*;

/**
 * Коррелатный способ для нивелирных сетей.
 * Формирует условия замкнутых полигонов через остовное дерево.
 */
@Slf4j
public class CorrelateEngine extends AbstractEngine {

    public CorrelateEngine(NormalEquationAssembler assembler) {
        super(assembler);
    }

    public AdjustmentEngine asEngine() {
        return new Adapter(this);
    }

    public AdjustmentResult adjustInternal(Network network, double[] variances,
                                           AdjustmentOptions options) {
        long start = System.currentTimeMillis();

        List<List<Integer>> cycles = findFundamentalCycles(network);
        int r = cycles.size();
        if (r == 0) {
            log.warn("Замкнутых циклов нет — fallback к параметрическому");
            return new ParametricEngine(assembler, null)
                    .adjustInternal(network, variances, options);
        }

        int n = network.measurements().size();

        SimpleMatrix B = new SimpleMatrix(r, n);
        SimpleMatrix W = new SimpleMatrix(r, 1);

        for (int i = 0; i < r; i++) {
            List<Integer> cycle = cycles.get(i);
            double closure = 0;
            for (int measIdx : cycle) {
                Measurement m = network.measurements().get(measIdx);
                B.set(i, measIdx, 1.0);
                closure += m.getObserved();
            }
            W.set(i, 0, closure);
        }

        SimpleMatrix PInv = new SimpleMatrix(n, n);
        for (int i = 0; i < n; i++) PInv.set(i, i, variances[i]);

        SimpleMatrix N = B.mult(PInv).mult(B.transpose());

        SimpleMatrix K;
        try {
            K = N.invert().mult(W.scale(-1));
        } catch (Exception e) {
            log.warn("Коррелатная N вырождена — fallback");
            return new ParametricEngine(assembler, null)
                    .adjustInternal(network, variances, options);
        }

        SimpleMatrix V = PInv.mult(B.transpose()).mult(K);

        // Создаём НОВЫЕ измерения с исправленными наблюдениями
        List<Measurement> corrected = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Measurement original = network.measurements().get(i);
            double newObs = original.getObserved() + V.get(i, 0);

            Measurement updated;
            if (original instanceof LevelingMeasurement lm) {
                updated = new LevelingMeasurement(
                        newObs, lm.getVariance(),
                        lm.fromPoint(), lm.toPoint(),
                        ParameterSet.fromPoints(network.points()),
                        lm.accuracyClass(), lm.instrument(), lm.tag());
            } else {
                updated = original;  // не должно быть, но защита
            }
            corrected.add(updated);
        }

        Network correctedNetwork = network.withMeasurements(corrected);

        double[] X = initParameters(correctedNetwork);
        ParameterSet params = ParameterSet.fromPoints(correctedNetwork.points());

        var eq = assembler.assemble(correctedNetwork, params, X);
        SimpleMatrix delta = eq.N().solve(eq.b());
        for (int j = 0; j < X.length; j++) X[j] += delta.get(j, 0);

        SimpleMatrix Q = eq.N().invert();
        double refErr = computeReferenceError(correctedNetwork, X, variances);

        return buildResult(correctedNetwork, X, Q, refErr, 1, true,
                List.of(), "CORRELATE",
                System.currentTimeMillis() - start);
    }

    private List<List<Integer>> findFundamentalCycles(Network network) {
        List<List<Integer>> cycles = new ArrayList<>();
        List<Measurement> meas = network.measurements();

        // Граф: точка -> [(индекс соседа, индекс измерения)]
        Map<String, List<int[]>> adj = new HashMap<>();
        Map<String, Integer> pointIdx = new HashMap<>();
        int idx = 0;
        for (var p : network.points()) pointIdx.put(p.id(), idx++);

        for (int i = 0; i < meas.size(); i++) {
            if (!(meas.get(i) instanceof LevelingMeasurement lm)) continue;
            String from = lm.fromPoint(), to = lm.toPoint();
            int fi = pointIdx.get(from);
            int ti = pointIdx.get(to);
            adj.computeIfAbsent(from, k -> new ArrayList<>()).add(new int[]{ti, i});
            adj.computeIfAbsent(to, k -> new ArrayList<>()).add(new int[]{fi, i});
        }

        // Ограничим число циклов r = n - points + 1
        int maxCycles = Math.max(1, meas.size() - network.points().size() + 1);

        Set<String> visited = new HashSet<>();
        for (var p : network.points()) {
            if (!visited.contains(p.id())) {
                dfsCycle(p.id(), null, adj, pointIdx, visited,
                        new ArrayList<>(), cycles, maxCycles);
            }
            if (cycles.size() >= maxCycles) break;
        }

        return cycles.size() > maxCycles ? cycles.subList(0, maxCycles) : cycles;
    }

    private void dfsCycle(String node, String parent,
                          Map<String, List<int[]>> adj,
                          Map<String, Integer> pointIdx,
                          Set<String> visited,
                          List<Integer> path,
                          List<List<Integer>> cycles,
                          int maxCycles) {
        if (cycles.size() >= maxCycles) return;

        visited.add(node);
        for (int[] edge : adj.getOrDefault(node, List.of())) {
            String neighbor = reverseLookup(edge[0], pointIdx);
            if (neighbor == null || neighbor.equals(parent)) continue;

            if (path.contains(edge[1])) continue;

            if (visited.contains(neighbor)) {
                if (path.size() > 1) {
                    List<Integer> cycle = new ArrayList<>(path);
                    cycle.add(edge[1]);
                    cycles.add(cycle);
                }
            } else {
                path.add(edge[1]);
                dfsCycle(neighbor, node, adj, pointIdx, visited, path, cycles, maxCycles);
                path.remove(path.size() - 1);
            }
        }
    }

    private String reverseLookup(int idx, Map<String, Integer> map) {
        for (var e : map.entrySet()) {
            if (e.getValue() == idx) return e.getKey();
        }
        return null;
    }

    private static final class Adapter implements AdjustmentEngine {
        private final CorrelateEngine delegate;

        Adapter(CorrelateEngine delegate) {
            this.delegate = delegate;
        }

        @Override
        public AdjustmentResult adjust(Network network, double[] initialVar,
                                       AdjustmentOptions options) {
            return delegate.adjustInternal(network, initialVar, options);
        }

        @Override
        public String methodName() {
            return "CORRELATE";
        }
    }
}