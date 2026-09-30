package ru.geodesy.adjust.design.api;

import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.List;
import java.util.Map;

public record OptimizationResult(
        List<Measurement> selectedMeasurements,
        double totalCost,
        Map<String, double[]> projectedStdDevs,
        double fitness,
        int generationsUsed,
        boolean budgetExceeded,       // ← добавлено
        long elapsedMillis
) {}