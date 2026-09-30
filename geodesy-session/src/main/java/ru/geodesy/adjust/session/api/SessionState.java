package ru.geodesy.adjust.session.api;

import ru.geodesy.adjust.adjustment.api.AdjustmentResult;

import java.time.Instant;
import java.util.Map;

/**
 * Текущее состояние сессии.
 */
public record SessionState(
        SessionId id,
        Instant createdAt,
        Instant lastAccessAt,
        int measurementCount,
        int pointCount,
        double referenceError,
        boolean dirty,
        int version,
        Map<String, double[]> currentCoordinates,
        AdjustmentResult lastResult
) {}