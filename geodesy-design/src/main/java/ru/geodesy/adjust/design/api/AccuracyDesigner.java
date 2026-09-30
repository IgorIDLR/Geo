package ru.geodesy.adjust.design.api;

/**
 * Публичный API обратной задачи — проектирования точности.
 */
public interface AccuracyDesigner {

    DesignResult design(DesignRequest request);
}