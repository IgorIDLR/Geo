package ru.geodesy.adjust.design.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.design.api.DesignRequest;
import ru.geodesy.adjust.design.api.DesignResult;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Итеративное уточнение весов измерений для достижения заданной точности.
 * <p>
 * Измерения не мутируются — работаем с локальным массивом весов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeightAdjuster {

    private final PrecisionCalculator calculator;
    private final DesignProperties props;

    public DesignResult adjust(DesignRequest request) {
        long start = System.currentTimeMillis();

        Network network = request.network();
        double[] variances = request.variances().clone();
        Map<String, double[]> targets = request.targets();

        Map<String, Double> initialDeficits = computeDeficits(
                network, variances, targets, request);

        double bestMaxDeficit = Double.MAX_VALUE;
        double[] bestVariances = variances.clone();

        for (int iter = 0; iter < request.maxIterations(); iter++) {
            Map<String, double[]> currentStd = computeStd(network, variances, request);
            boolean allOk = true;
            double maxDeficit = 0;

            for (var targetEntry : targets.entrySet()) {
                String pointId = targetEntry.getKey();
                double[] reqStd = targetEntry.getValue();
                double[] curStd = currentStd.get(pointId);
                if (curStd == null) continue;

                for (int coord = 0; coord < 3; coord++) {
                    if (reqStd[coord] <= 0) continue;
                    double deficit = curStd[coord] / reqStd[coord];
                    maxDeficit = Math.max(maxDeficit, deficit);

                    if (deficit > 1.0) {
                        allOk = false;
                        double factor = clamp(deficit * deficit,
                                props.getMinWeightFactor(),
                                props.getMaxWeightFactor());

                        for (int i = 0; i < network.measurements().size(); i++) {
                            Measurement m = network.measurements().get(i);
                            if (involves(m, pointId)) {
                                variances[i] /= factor;
                            }
                        }
                    }
                }
            }

            log.debug("Итерация {}: max deficit = {}", iter + 1, maxDeficit);

            if (maxDeficit < bestMaxDeficit) {
                bestMaxDeficit = maxDeficit;
                bestVariances = variances.clone();
            }

            if (allOk) {
                Map<String, double[]> finalStd = computeStd(network, variances, request);
                Map<String, Double> finalDeficits = computeDeficits(
                        network, variances, targets, request);

                return new DesignResult(
                        true,
                        "Точность достигнута за " + (iter + 1) + " итераций",
                        iter + 1,
                        variances,
                        java.util.List.of(),
                        finalStd,
                        initialDeficits,
                        finalDeficits,
                        System.currentTimeMillis() - start
                );
            }
        }

        Map<String, double[]> finalStd = computeStd(network, bestVariances, request);
        Map<String, Double> finalDeficits = computeDeficits(
                network, bestVariances, targets, request);

        return new DesignResult(
                false,
                String.format("Точность не достигнута за %d итераций "
                                + "(дефицит = %.3f)",
                        request.maxIterations(), bestMaxDeficit),
                request.maxIterations(),
                bestVariances,
                java.util.List.of(),
                finalStd,
                initialDeficits,
                finalDeficits,
                System.currentTimeMillis() - start
        );
    }

    private Map<String, double[]> computeStd(Network network,
                                             double[] variances,
                                             DesignRequest request) {
        SimpleMatrix Q = calculator.computeQ(network, variances);
        return calculator.extractStdDevs(network, Q, network.sigma0());
    }

    private Map<String, Double> computeDeficits(Network network,
                                                double[] variances,
                                                Map<String, double[]> targets,
                                                DesignRequest request) {
        Map<String, double[]> current = computeStd(network, variances, request);
        Map<String, Double> deficits = new LinkedHashMap<>();

        for (var e : targets.entrySet()) {
            String pointId = e.getKey();
            double[] req = e.getValue();
            double[] cur = current.get(pointId);
            if (cur == null) continue;

            double maxDef = 0;
            for (int i = 0; i < 3; i++) {
                if (req[i] > 0) {
                    maxDef = Math.max(maxDef, cur[i] / req[i]);
                }
            }
            deficits.put(pointId, maxDef);
        }
        return deficits;
    }

    private boolean involves(Measurement m, String pointId) {
        for (String id : m.involvedPointIds()) {
            if (id.equals(pointId)) return true;
        }
        return false;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}