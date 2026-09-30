package ru.geodesy.adjust.adjustment.api;

import ru.geodesy.adjust.model.Network;

/**
 * Публичный интерфейс движка уравнивания.
 */
public interface AdjustmentEngine {

    AdjustmentResult adjust(Network network, double[] initialVar,
                            AdjustmentOptions options);

    String methodName();
}