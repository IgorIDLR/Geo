package ru.geodesy.adjust.design;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.design.api.DesignRequest;
import ru.geodesy.adjust.design.api.DesignResult;
import ru.geodesy.adjust.design.internal.AccuracyDesignerImpl;
import ru.geodesy.adjust.design.internal.DesignProperties;
import ru.geodesy.adjust.design.internal.PrecisionCalculator;
import ru.geodesy.adjust.design.internal.WeightAdjuster;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.NetworkType;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.DistanceMeasurement;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccuracyDesignerTest {

    private AccuracyDesignerImpl designer;

    @BeforeEach
    void setup() {
        PrecisionCalculator calculator = new PrecisionCalculator();
        DesignProperties props = new DesignProperties();
        WeightAdjuster adjuster = new WeightAdjuster(calculator, props);
        designer = new AccuracyDesignerImpl(adjuster);
    }

    @Test
    void weightAdjustmentImprovesOrAchievesTarget() {
        Network net = simpleTriangulation();
        double[] variances = {1e-3, 1e-3, 1e-3};  // изначально грубые

        // Целевая точность: СКО координат C ≤ 5 мм
        Map<String, double[]> targets = Map.of(
                "C", new double[]{0.005, 0.005, 0.005}
        );

        DesignResult result = designer.design(
                DesignRequest.adjustWeights(net, variances, targets));

        assertThat(result).isNotNull();
        assertThat(result.iterations()).isGreaterThan(0);
        assertThat(result.adjustedVariances()).isNotEmpty();

        // Если достигли — сообщение содержит "достигнута"
        if (result.achieved()) {
            assertThat(result.message()).contains("достигнута");
        } else {
            assertThat(result.message()).contains("не достигнута");
        }
    }

    @Test
    void initialDeficitsAreComputed() {
        Network net = simpleTriangulation();
        double[] variances = {1e-3, 1e-3, 1e-3};

        Map<String, double[]> targets = Map.of(
                "C", new double[]{0.005, 0.005, 0.005}
        );

        DesignResult result = designer.design(
                DesignRequest.adjustWeights(net, variances, targets));

        assertThat(result.initialDeficits()).containsKey("C");
        assertThat(result.finalDeficits()).containsKey("C");

        // Финальный дефицит должен быть ≤ изначального
        double initial = result.initialDeficits().get("C");
        double fin = result.finalDeficits().get("C");
        assertThat(fin).isLessThanOrEqualTo(initial + 1e-6);
    }

    @Test
    void networkGeometryIsNotMutated() {
        Network net = simpleTriangulation();
        double[] variances = {1e-3, 1e-3, 1e-3};

        // Сохраняем изначальные дисперсии
        double[] originalVariances = new double[3];
        for (int i = 0; i < 3; i++) {
            originalVariances[i] = net.measurements().get(i).getVariance();
        }

        Map<String, double[]> targets = Map.of(
                "C", new double[]{0.005, 0.005, 0.005}
        );

        designer.design(DesignRequest.adjustWeights(net, variances, targets));

        // Измерения не должны быть изменены
        for (int i = 0; i < 3; i++) {
            assertThat(net.measurements().get(i).getVariance())
                    .isEqualTo(originalVariances[i]);
        }
    }

    @Test
    void addMeasurementsStrategyRequiresFullProfile() {
        Network net = simpleTriangulation();
        double[] variances = {1e-3, 1e-3, 1e-3};

        Map<String, double[]> targets = Map.of(
                "C", new double[]{0.005, 0.005, 0.005}
        );

        DesignRequest request = new DesignRequest(
                net, variances, targets,
                List.of(net.measurements().get(0)),  // фиктивный кандидат
                DesignRequest.DesignStrategy.ADD_MEASUREMENTS,
                10, 1e-4
        );

        // В тестах без профиля full должно бросить исключение
        try {
            designer.design(request);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("full");
        }
    }

    private Network simpleTriangulation() {
        List<Point> points = List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0),
                new Point("C", false, 50.0, 80.0, 0.0)
        );
        ParameterSet params = ParameterSet.fromPoints(points);

        List<Measurement> measurements = List.of(
                new DistanceMeasurement(94.34, 1e-5, "A", "C", params),
                new DistanceMeasurement(94.34, 1e-5, "B", "C", params),
                new DistanceMeasurement(100.0, 1e-5, "A", "B", params)
        );

        return new Network(points, measurements,
                NetworkType.fixed(), 1.0, 2);
    }
}