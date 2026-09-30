package ru.geodesy.adjust.session.api;

import java.util.List;

/**
 * Результат применения патчей к сессии.
 */
public record PatchResult(
        boolean applied,
        int appliedPatches,
        int rejectedPatches,
        List<String> errors,
        double referenceError,
        int iterations,
        long elapsedMillis,
        boolean fullRecalculationRequired
) {}