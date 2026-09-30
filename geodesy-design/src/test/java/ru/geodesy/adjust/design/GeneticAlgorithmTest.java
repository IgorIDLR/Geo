package ru.geodesy.adjust.design;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import ru.geodesy.adjust.design.api.OptimizationRequest;
import ru.geodesy.adjust.design.api.OptimizationResult;
import ru.geodesy.adjust.design.internal.GeneticAlgorithm;
import ru.geodesy.adjust.design.internal.PrecisionCalculator;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.NetworkType;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.DistanceMeasurement;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты генетического алгоритма.
 * Активны только при наличии Jenetics (профиль full).
 *
 * Запуск:
 *   mvn -pl geodesy-design test -Pfull -Dgeodesy.profile=full
 */
@EnabledIfSystemProperty(named = "geodesy.profile", matches = "full")
class GeneticAlgorithmTest {

    private GeneticAlgorithm algorithm;

    @BeforeEach
    void setup() {
        PrecisionCalculator calculator = new PrecisionCalculator();
        ForkJoinPool executor = new ForkJoinPool(4);
        algorithm = new GeneticAlgorithm(calculator, executor);
    }

    @Test
    void findsValidSubsetOfCandidates() {
        Network base = baseNetwork();
        List<Measurement> candidates = candidateMeasurements(base);

        OptimizationRequest request = new OptimizationRequest(
                base,
                new double[]{1e-5},
                candidates,
                3.0,
                OptimizationRequest.CostFunction.uniform(),
                30, 20, 0.15
        );

        OptimizationResult result = algorithm.optimize(request);

        assertThat(result.selectedMeasurements()).isNotEmpty();
        assertThat(result.totalCost()).isLessThanOrEqualTo(request.budget());
        assertThat(result.projectedStdDevs()).containsKey("C");
        assertThat(result.generationsUsed()).isPositive();
        assertThat(result.elapsedMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void emptyCandidatesThrows() {
        Network base = baseNetwork();
        assertThatThrownBy(() -> new OptimizationRequest(
                base,
                new double[]{1e-5},
                List.of(),
                1.0,
                OptimizationRequest.CostFunction.uniform(),
                30, 10, 0.15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("пуст");
    }

    @Test
    void budgetExceededReportedAsFlag() {
        Network base = baseNetwork();
        List<Measurement> candidates = candidateMeasurements(base);

        // Маленький бюджет — часть кандидатов не поместится
        OptimizationRequest request = new OptimizationRequest(
                base, new double[]{1e-5}, candidates,
                0.5, OptimizationRequest.CostFunction.uniform(),
                20, 10, 0.15);

        OptimizationResult result = algorithm.optimize(request);

        if (result.totalCost() > request.budget()) {
            assertThat(result.budgetExceeded()).isTrue();
        }
    }

    @Test
    void tooFewPopulationThrows() {
        Network base = baseNetwork();
        List<Measurement> candidates = candidateMeasurements(base);

        // Population size < 3 — TournamentSelector не сможет работать
        assertThatThrownBy(() -> new OptimizationRequest(
                base, new double[]{1e-5}, candidates,
                3.0, OptimizationRequest.CostFunction.uniform(),
                2, 10, 0.15))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeBudgetThrows() {
        Network base = baseNetwork();
        List<Measurement> candidates = candidateMeasurements(base);

        assertThatThrownBy(() -> new OptimizationRequest(
                base, new double[]{1e-5}, candidates,
                -1.0, OptimizationRequest.CostFunction.uniform(),
                30, 10, 0.15))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ================================================================
    // Вспомогательные
    // ================================================================

    private Network baseNetwork() {
        List<Point> points = List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0),
                new Point("C", false, 50.0, 80.0, 0.0)
        );
        ParameterSet params = ParameterSet.fromPoints(points);

        List<Measurement> base = List.of(
                new DistanceMeasurement(100.0, 1e-5, "A", "B", params)
        );

        return new Network(points, base, NetworkType.fixed(), 1.0, 2);
    }

    private List<Measurement> candidateMeasurements(Network base) {
        ParameterSet params = ParameterSet.fromPoints(base.points());
        return List.of(
                new DistanceMeasurement(94.34, 1e-5, "A", "C", params),
                new DistanceMeasurement(94.34, 1e-5, "B", "C", params),
                new DistanceMeasurement(50.0,  1e-5, "A", "C", params),
                new DistanceMeasurement(50.0,  1e-5, "B", "C", params),
                new DistanceMeasurement(100.0, 1e-5, "B", "A", params)
        );
    }
}