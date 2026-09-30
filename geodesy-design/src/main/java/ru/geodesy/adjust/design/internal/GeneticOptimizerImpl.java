package ru.geodesy.adjust.design.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ru.geodesy.adjust.design.api.GeneticOptimizer;
import ru.geodesy.adjust.design.api.OptimizationRequest;
import ru.geodesy.adjust.design.api.OptimizationResult;

/**
 * Реализация генетического оптимизатора (профиль full).
 */
@Slf4j
@Service
@Profile("full")
@RequiredArgsConstructor
public class GeneticOptimizerImpl implements GeneticOptimizer {

    private final GeneticAlgorithm geneticAlgorithm;

    @Override
    public OptimizationResult optimize(OptimizationRequest request) {
        return geneticAlgorithm.optimize(request);
    }
}