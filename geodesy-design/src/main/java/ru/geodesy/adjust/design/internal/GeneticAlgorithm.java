package ru.geodesy.adjust.design.internal;

import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.design.api.OptimizationRequest;
import ru.geodesy.adjust.design.api.OptimizationResult;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.common.DesignException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

/**
 * Генетический алгоритм оптимизации набора измерений при
 * ограниченном бюджете.
 * <p>
 * Активен только в профиле full (требует Jenetics).
 */
@Slf4j
@Component
@Profile("full")
@RequiredArgsConstructor
public class GeneticAlgorithm {

    private final PrecisionCalculator calculator;

    /**
     * Общий пул потоков для параллельной оценки популяции.
     * Инжектируется Spring и переиспользуется между вызовами.
     */
    private final ForkJoinPool designExecutor;

    public OptimizationResult optimize(OptimizationRequest request) {
        long start = System.currentTimeMillis();
        int n = request.candidateMeasurements().size();

        // ---------- 1. Валидация входных данных ----------
        if (n == 0) {
            throw new DesignException("Список кандидатов пуст");
        }
        if (request.populationSize() < 3) {
            throw new DesignException(
                    "Размер популяции должен быть ≥ 3, получено "
                            + request.populationSize());
        }

        log.info("Генетическая оптимизация: кандидатов = {}, "
                        + "бюджет = {}, популяция = {}, поколений = {}",
                n, request.budget(),
                request.populationSize(), request.generations());

        // ---------- 2. Подготовка функции приспособленности ----------
        FitnessFunction fitness = new FitnessFunction(request, calculator);

        // ✅ Адаптер: Genotype<BitGene> → BitSet
        Function<Genotype<BitGene>, Double> fitnessFunc = gt -> {
            BitChromosome chromosome = (BitChromosome) gt.chromosome();
            return fitness.evaluate(chromosome.toBitSet());
        };

        Factory<Genotype<BitGene>> genotypeFactory =
                Genotype.of(BitChromosome.of(n));

        // ---------- 3. Движок эволюции ----------
        Engine<BitGene, Double> engine = Engine
                .builder(fitnessFunc, genotypeFactory)
                .populationSize(request.populationSize())
                .selector(new TournamentSelector<>(3))
                .alterers(
                        new Mutator<BitGene, Double>(request.mutationRate()),
                        new SinglePointCrossover<BitGene, Double>(0.5))
                .minimizing()
                .executor(designExecutor)
                .build();

        // ---------- 4. Запуск эволюции ----------
        EvolutionResult<BitGene, Double> result;
        try {
            result = engine.stream()
                    .limit(request.generations())
                    .collect(EvolutionResult.toBestEvolutionResult());
        } catch (Exception e) {
            throw new DesignException(
                    "Генетическая эволюция не удалась: " + e.getMessage(), e);
        }

        // ---------- 5. Проверка результата ----------
        Double bestFitness = result.bestFitness();
        Phenotype<BitGene, Double> bestPhenotype = result.bestPhenotype();

        if (bestFitness == null || bestPhenotype == null) {
            throw new DesignException(
                    "Генетический алгоритм не вернул результат — "
                            + "проверьте параметры популяции и поколений");
        }

        // ---------- 6. Извлечение выбранных измерений ----------
        BitChromosome chromosome =
                (BitChromosome) bestPhenotype.genotype().chromosome();
        BitSet bits = chromosome.toBitSet();

        List<Measurement> selected = new ArrayList<>();
        double totalCost = 0;
        for (int i = 0; i < n; i++) {
            if (bits.get(i)) {
                Measurement m = request.candidateMeasurements().get(i);
                selected.add(m);
                totalCost += request.costFunction().cost(m);
            }
        }

        // ---------- 7. Проверка бюджета ----------
        boolean budgetExceeded = totalCost > request.budget();
        if (budgetExceeded) {
            log.warn("Итоговая стоимость {} превышает бюджет {} — "
                            + "увеличьте число поколений или размер популяции",
                    totalCost, request.budget());
        }

        // ---------- 8. Финальная оценка точности ----------
        double sigma0 = request.baseNetwork().sigma0();
        Map<String, double[]> stds = estimateStdDevs(
                request, selected, sigma0);

        // ---------- 9. Формирование результата ----------
        int generationsUsed = (int) result.totalGenerations();

        log.info("Эволюция завершена: fitness = {}, "
                        + "выбрано {} из {}, стоимость = {} (бюджет = {}), "
                        + "поколений = {}, время = {} мс",
                bestFitness, selected.size(), n,
                totalCost, request.budget(),
                generationsUsed,
                System.currentTimeMillis() - start);

        return new OptimizationResult(
                selected,
                totalCost,
                stds,
                bestFitness,
                generationsUsed,
                budgetExceeded,
                System.currentTimeMillis() - start
        );
    }

    /**
     * Оценка СКО координат для выбранного набора измерений.
     */
    private Map<String, double[]> estimateStdDevs(
            OptimizationRequest request,
            List<Measurement> selected,
            double sigma0) {

        List<Measurement> all = new ArrayList<>(
                request.baseNetwork().measurements());
        all.addAll(selected);

        Network augmented = new Network(
                request.baseNetwork().points(),
                all,
                request.baseNetwork().networkType(),
                sigma0,
                request.baseNetwork().dimension());

        double[] variances = new double[all.size()];
        System.arraycopy(request.baseVariances(), 0,
                variances, 0, request.baseVariances().length);
        for (int i = 0; i < selected.size(); i++) {
            variances[request.baseVariances().length + i] =
                    selected.get(i).getVariance();
        }

        var Q = calculator.computeQ(augmented, variances);
        return calculator.extractStdDevs(augmented, Q, sigma0);
    }
}