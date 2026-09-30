package ru.geodesy.adjust.deformation.api;

/**
 * Публичный API анализа деформаций.
 */
public interface DeformationAnalysisService {

    /**
     * Выполняет анализ деформаций между эпохами.
     */
    DeformationResult analyze(DeformationRequest request);
}