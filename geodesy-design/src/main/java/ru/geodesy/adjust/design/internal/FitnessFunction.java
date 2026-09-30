package ru.geodesy.adjust.design.internal;

import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.design.api.OptimizationRequest;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Функция приспособленности для генетического алгоритма:
 * penalty (превышение бюджета) + maxStdDev.
 */
public class FitnessFunction {

    private final OptimizationRequest request;
    private final PrecisionCalculator calculator;
    private final List<Measurement> candidates;
    private final double[] candidateCosts;
    private final double[] baseVariances;
    private final int candidateCount;

    public FitnessFunction(OptimizationRequest request, PrecisionCalculator calculator) {
        this.request = request;
        this.calculator = calculator;
        this.candidates = request.candidateMeasurements();
        this.candidateCount = candidates.size();
        this.baseVariances = request.baseVariances().clone();
        this.candidateCosts = new double[candidateCount];
        for (int i = 0; i < candidateCount; i++) {
            candidateCosts[i] = request.costFunction().cost(candidates.get(i));
        }
    }

    public double evaluate(BitSet bits) {
        List<Measurement> selected = new ArrayList<>();
        double totalCost = 0;

        for (int i = 0; i < candidateCount; i++) {
            if (bits.get(i)) {
                selected.add(candidates.get(i));
                totalCost += candidateCosts[i];
            }
        }

        double overBudget = Math.max(0, totalCost - request.budget());
        double penalty = overBudget * 1000.0;

        Network augmented = augmentNetwork(request.baseNetwork(), selected);
        double[] variances = augmentVariances(baseVariances, selected);

        try {
            SimpleMatrix Q = calculator.computeQ(augmented, variances);
            Map<String, double[]> stds = calculator.extractStdDevs(
                    augmented, Q, augmented.sigma0());

            double maxStd = 0;
            for (var e : stds.entrySet()) {
                for (double v : e.getValue()) {
                    maxStd = Math.max(maxStd, v);
                }
            }
            return penalty + maxStd;
        } catch (Exception ex) {
            return penalty + 1e6;
        }
    }

    private Network augmentNetwork(Network base, List<Measurement> additional) {
        List<Measurement> all = new ArrayList<>(base.measurements());
        all.addAll(additional);
        return new Network(base.points(), all, base.networkType(),
                base.sigma0(), base.dimension());
    }

    private double[] augmentVariances(double[] base, List<Measurement> additional) {
        double[] result = new double[base.length + additional.size()];
        System.arraycopy(base, 0, result, 0, base.length);
        for (int i = 0; i < additional.size(); i++) {
            result[base.length + i] = additional.get(i).getVariance();
        }
        return result;
    }
}