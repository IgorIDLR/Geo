package ru.geodesy.adjust.design.api;

/**
 * Публичный API генетической оптимизации.
 * Активен только в профиле full (требует Jenetics).
 */
public interface GeneticOptimizer {

    OptimizationResult optimize(OptimizationRequest request);
}