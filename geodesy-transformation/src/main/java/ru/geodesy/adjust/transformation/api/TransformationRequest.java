package ru.geodesy.adjust.transformation.api;

import ru.geodesy.common.Preconditions;

/**
 * Запрос на трансформацию.
 */
public record TransformationRequest(
        TransformationMethod method,
        String fromSystemId,
        String toSystemId,
        double[][] fromPoints,
        double[][] toPoints,
        boolean applyReduction
) {

    public TransformationRequest {
        Preconditions.requireNonNull(method, "transformation.method");
        Preconditions.requireNonBlank(fromSystemId, "transformation.fromSystem");
        Preconditions.requireNonBlank(toSystemId, "transformation.toSystem");

        if (method == TransformationMethod.BY_POINTS) {
            if (fromPoints == null || toPoints == null) {
                throw new IllegalArgumentException(
                        "BY_POINTS требует fromPoints и toPoints");
            }
            if (fromPoints.length != toPoints.length || fromPoints.length < 3) {
                throw new IllegalArgumentException(
                        "Для BY_POINTS нужно минимум 3 общие точки");
            }
        }
    }

    public static TransformationRequest simple(TransformationMethod method,
                                               String fromId, String toId) {
        return new TransformationRequest(method, fromId, toId, null, null, false);
    }
}