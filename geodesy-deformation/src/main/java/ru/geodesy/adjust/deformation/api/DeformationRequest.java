package ru.geodesy.adjust.deformation.api;

import ru.geodesy.common.Preconditions;

import java.util.List;

/**
 * Запрос на анализ деформаций.
 */
public record DeformationRequest(
        List<EpochData> epochs,
        String method,
        double significanceLevel,
        String referencePoints,
        Integer datumDegree
) {

    public static final String METHOD_SQUARED_MSPLIT = "SQUARED_MSPLIT";
    public static final String METHOD_MSPLIT_CD = "MSPLIT_CD";
    public static final String METHOD_PSEUDO_EPOCH = "PSEUDO_EPOCH";

    public DeformationRequest {
        Preconditions.requireNonNull(epochs, "deformation.epochs");
        if (epochs.size() < 2) {
            throw new IllegalArgumentException(
                    "Для анализа деформаций нужно минимум 2 эпохи");
        }
        if (method == null || method.isBlank()) method = METHOD_SQUARED_MSPLIT;
        if (significanceLevel <= 0 || significanceLevel >= 1) significanceLevel = 0.05;
        if (referencePoints == null) referencePoints = "STABLE";
        if (datumDegree == null) datumDegree = 3;

        epochs = List.copyOf(epochs);
    }

    public static DeformationRequest of(List<EpochData> epochs) {
        return new DeformationRequest(epochs, METHOD_SQUARED_MSPLIT,
                0.05, "STABLE", 3);
    }
}