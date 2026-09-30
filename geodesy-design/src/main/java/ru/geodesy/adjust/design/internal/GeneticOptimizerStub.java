package ru.geodesy.adjust.design.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ru.geodesy.adjust.design.api.GeneticOptimizer;
import ru.geodesy.adjust.design.api.OptimizationRequest;
import ru.geodesy.adjust.design.api.OptimizationResult;
import ru.geodesy.common.DesignException;

/**
 * Заглушка для профиля lite.
 * Генетическая оптимизация доступна только в профиле full.
 */
@Slf4j
@Service
@Profile("!full")
public class GeneticOptimizerStub implements GeneticOptimizer {

    @Override
    public OptimizationResult optimize(OptimizationRequest request) {
        throw new DesignException(
                "Генетическая оптимизация доступна только в профиле 'full'. "
                        + "Запустите приложение с -Pfull "
                        + "или spring.profiles.active=full");
    }
}