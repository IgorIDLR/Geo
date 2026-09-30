package ru.geodesy.adjust.deformation.api;

import ru.geodesy.common.Preconditions;
import java.time.Instant;

/**
 * Данные одной эпохи измерений.
 */
public record EpochData(
        String epochId,
        Instant timestamp,
        String networkRawData
) {
    public EpochData {
        Preconditions.requireNonBlank(epochId, "epoch.id");
        Preconditions.requireNonBlank(networkRawData, "epoch.rawData");
    }
}