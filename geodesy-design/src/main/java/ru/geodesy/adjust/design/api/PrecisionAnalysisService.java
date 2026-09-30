package ru.geodesy.adjust.design.api;

/**
 * Публичный API прямой задачи — анализа ожидаемой точности.
 */
public interface PrecisionAnalysisService {

    /**
     * Рассчитывает ожидаемую точность координат всех точек сети
     * без фактического уравнивания.
     */
    PrecisionResult analyze(PrecisionRequest request);
}