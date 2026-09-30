package ru.geodesy.adjust.design.api;

import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.common.Preconditions;

import java.util.List;
import java.util.Map;

/**
 * Запрос на обратную задачу — проектирование точности.
 */
public record DesignRequest(
        Network network,
        double[] variances,
        Map<String, double[]> targets,
        List<Measurement> candidateMeasurements,
        DesignStrategy strategy,
        int maxIterations,
        double convergenceEps
) {

    public enum DesignStrategy {
        ADJUST_WEIGHTS,
        ADD_MEASUREMENTS,
        COMBINED
    }

    public DesignRequest {
        Preconditions.requireNonNull(network, "design.network");
        Preconditions.requireNonNull(targets, "design.targets");
        Preconditions.requireNonNull(strategy, "design.strategy");

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("Список целевых точностей пуст");
        }
        if (maxIterations <= 0) maxIterations = 20;
        if (convergenceEps <= 0) convergenceEps = 1e-4;

        if (variances != null) variances = variances.clone();

        if (strategy == DesignStrategy.ADD_MEASUREMENTS
                || strategy == DesignStrategy.COMBINED) {
            if (candidateMeasurements == null || candidateMeasurements.isEmpty()) {
                throw new IllegalArgumentException(
                        "Для стратегии " + strategy + " нужен список кандидатов");
            }
        }
    }

    public static DesignRequest adjustWeights(Network net, double[] vars,
                                              Map<String, double[]> targets) {
        return new DesignRequest(net, vars, targets, List.of(),
                DesignStrategy.ADJUST_WEIGHTS, 20, 1e-4);
    }

    public static DesignRequest addMeasurements(Network net, double[] vars,
                                                Map<String, double[]> targets,
                                                List<Measurement> candidates) {
        return new DesignRequest(net, vars, targets, candidates,
                DesignStrategy.ADD_MEASUREMENTS, 20, 1e-4);
    }
}