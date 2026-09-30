package ru.geodesy.adjust.design.api;

import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.common.Preconditions;

import java.util.List;

/**
 * Запрос на генетическую оптимизацию набора измерений.
 */
public record OptimizationRequest(
        Network baseNetwork,
        double[] baseVariances,
        List<Measurement> candidateMeasurements,
        double budget,
        CostFunction costFunction,
        int populationSize,
        int generations,
        double mutationRate
) {

    @FunctionalInterface
    public interface CostFunction {
        double cost(Measurement measurement);

        static CostFunction uniform() {
            return m -> 1.0;
        }

        static CostFunction byVariance() {
            return Measurement::getVariance;
        }
    }

    public OptimizationRequest {
        Preconditions.requireNonNull(baseNetwork, "optimization.baseNetwork");
        Preconditions.requireNonNull(candidateMeasurements, "optimization.candidates");
        Preconditions.requirePositive(budget, "optimization.budget");

        if (candidateMeasurements.isEmpty()) {
            throw new IllegalArgumentException("Список кандидатов пуст");
        }
        if (costFunction == null) costFunction = CostFunction.uniform();

        if (populationSize < 3) populationSize = 100;
        if (generations <= 0) generations = 50;
        if (mutationRate < 0 || mutationRate > 1) mutationRate = 0.1;

        if (baseVariances != null) baseVariances = baseVariances.clone();
    }
}