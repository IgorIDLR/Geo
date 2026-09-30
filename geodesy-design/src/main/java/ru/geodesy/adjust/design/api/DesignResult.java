package ru.geodesy.adjust.design.api;

import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.List;
import java.util.Map;

/**
 * Результат проектирования точности.
 */
public record DesignResult(
        boolean achieved,
        String message,
        int iterations,
        double[] adjustedVariances,
        List<Measurement> addedMeasurements,
        Map<String, double[]> achievedStdDevs,
        Map<String, Double> initialDeficits,
        Map<String, Double> finalDeficits,
        long elapsedMillis
) {

    public static DesignResult failure(String message) {
        return new DesignResult(false, message, 0,
                new double[0], List.of(), Map.of(), Map.of(), Map.of(), 0);
    }
}