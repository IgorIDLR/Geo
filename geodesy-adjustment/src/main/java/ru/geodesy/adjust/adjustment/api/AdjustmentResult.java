package ru.geodesy.adjust.adjustment.api;

import java.util.List;
import java.util.Map;

/**
 * Результат уравнивания.
 * Поле {@code converged} отражает фактическую сходимость итерационного процесса.
 */
public record AdjustmentResult(
        Map<String, double[]> coordinates,
        Map<String, double[][]> covarianceBlocks,
        double referenceError,
        int iterations,
        boolean converged,
        List<String> suspectedBlunders,
        String methodUsed,
        long elapsedMillis
) {

    public static AdjustmentResult empty() {
        return new AdjustmentResult(Map.of(), Map.of(), 0.0, 0, false,
                List.of(), "NONE", 0);
    }
}