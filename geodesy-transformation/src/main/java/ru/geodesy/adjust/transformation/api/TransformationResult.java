package ru.geodesy.adjust.transformation.api;

import java.util.List;

/**
 * Результат трансформации.
 */
public record TransformationResult(
        TransformationMethod method,
        String fromSystemId,
        String toSystemId,
        List<double[]> transformedPoints,
        double[] transformationParameters,
        double rmsError,
        int iterations,
        long elapsedMillis
) {

    public static TransformationResult empty() {
        return new TransformationResult(
                TransformationMethod.NONE, "?", "?",
                List.of(), new double[0], 0.0, 0, 0);
    }
}