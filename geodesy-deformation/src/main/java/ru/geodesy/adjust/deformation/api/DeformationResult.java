package ru.geodesy.adjust.deformation.api;

import java.util.List;
import java.util.Map;

/**
 * Результат анализа деформаций.
 */
public record DeformationResult(
        String method,
        List<String> epochs,
        Map<String, List<PointDisplacement>> displacements,
        Map<String, String> pointClassification,
        Map<String, Double> groupAssignment,
        double globalTestStatistic,
        boolean deformationsDetected,
        long elapsedMillis
) {

    /**
     * Возвращает смещения конкретной точки.
     */
    public List<PointDisplacement> displacementsOf(String pointId) {
        return displacements.getOrDefault(pointId, List.of());
    }

    /**
     * Список неустойчивых точек.
     */
    public List<String> unstablePoints() {
        return pointClassification.entrySet().stream()
                .filter(e -> "UNSTABLE".equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Список устойчивых точек.
     */
    public List<String> stablePoints() {
        return pointClassification.entrySet().stream()
                .filter(e -> "STABLE".equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }
}