package ru.geodesy.adjust.design;

import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.design.internal.PrecisionCalculator;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.NetworkType;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.DistanceMeasurement;
import ru.geodesy.adjust.model.measurement.LevelingMeasurement;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PrecisionCalculatorTest {

    private PrecisionCalculator calculator;

    @BeforeEach
    void setup() {
        calculator = new PrecisionCalculator();
    }

    // ================================================================
    // 3D-сеть полного ранга
    // ================================================================

    @Test
    void qMatrixIsSymmetricAndPositiveDefinite() {
        Network net = fullRankNetwork();
        double[] variances = {1e-5, 1e-5, 1e-5, 1e-3};

        SimpleMatrix Q = calculator.computeQ(net, variances);
        int n = Q.numRows();
        assertThat(n).isEqualTo(3);

        // Симметричность
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                assertThat(Q.get(i, j))
                        .isCloseTo(Q.get(j, i), within(1e-12));
            }
        }

        // Все диагональные элементы положительны
        for (int i = 0; i < n; i++) {
            assertThat(Q.get(i, i)).isGreaterThan(0);
        }
    }

    @Test
    void smallerVarianceGivesSmallerStdDev() {
        Network net = fullRankNetwork();

        double[] varLow = {1e-8, 1e-8, 1e-8, 1e-6};
        double[] varHigh = {1e-3, 1e-3, 1e-3, 1e-1};

        SimpleMatrix QLow = calculator.computeQ(net, varLow);
        SimpleMatrix QHigh = calculator.computeQ(net, varHigh);

        for (int i = 0; i < QLow.numRows(); i++) {
            assertThat(QLow.get(i, i)).isLessThan(QHigh.get(i, i));
        }
    }

    // ================================================================
    // 2D-сеть (проверяем только X, Y)
    // ================================================================

    @Test
    void qMatrixFor2DNetworkIsPositiveSemiDefinite() {
        Network net = simpleTriangulationNetwork();
        double[] variances = {1e-5, 1e-5, 1e-5};

        SimpleMatrix Q = calculator.computeQ(net, variances);
        int n = Q.numRows();
        assertThat(n).isEqualTo(3);

        // X и Y определены
        assertThat(Q.get(0, 0)).isGreaterThan(0);
        assertThat(Q.get(1, 1)).isGreaterThan(0);
        // Z не определена — это корректно для 2D
        assertThat(Q.get(2, 2)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void smallerVarianceGivesSmallerStdDev2D() {
        Network net = simpleTriangulationNetwork();

        SimpleMatrix QLow = calculator.computeQ(net, new double[]{1e-8, 1e-8, 1e-8});
        SimpleMatrix QHigh = calculator.computeQ(net, new double[]{1e-3, 1e-3, 1e-3});

        // Только X и Y
        assertThat(QLow.get(0, 0)).isLessThan(QHigh.get(0, 0));
        assertThat(QLow.get(1, 1)).isLessThan(QHigh.get(1, 1));
    }

    @Test
    void extractStdDevsReturnsOnlyFreePoints() {
        Network net = simpleTriangulationNetwork();
        double[] variances = {1e-5, 1e-5, 1e-5};

        SimpleMatrix Q = calculator.computeQ(net, variances);
        Map<String, double[]> stds = calculator.extractStdDevs(net, Q, 1.0);

        assertThat(stds).containsKey("C");
        assertThat(stds).doesNotContainKey("A");
        assertThat(stds).doesNotContainKey("B");

        // X и Y должны быть > 0
        double[] cStd = stds.get("C");
        assertThat(cStd[0]).isGreaterThan(0);
        assertThat(cStd[1]).isGreaterThan(0);
    }

    @Test
    void extractErrorEllipsesAreValid() {
        Network net = fullRankNetwork();
        double[] variances = {1e-5, 1e-5, 1e-5, 1e-3};

        SimpleMatrix Q = calculator.computeQ(net, variances);
        Map<String, double[]> ellipses = calculator.extractErrorEllipses(net, Q, 1.0);

        for (var entry : ellipses.entrySet()) {
            double[] e = entry.getValue();
            assertThat(e).hasSize(3);
            assertThat(e[0]).isGreaterThanOrEqualTo(e[1]);
            assertThat(e[1]).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void freeNetworkProducesValidQ() {
        List<Point> points = List.of(
                new Point("A", false, 0.0, 0.0, 0.0),
                new Point("B", false, 100.0, 0.0, 0.0),
                new Point("C", false, 50.0, 80.0, 10.0)
        );
        ParameterSet params = ParameterSet.fromPoints(points);

        List<Measurement> measurements = List.of(
                new DistanceMeasurement(100.0, 1e-5, "A", "B", params),
                new DistanceMeasurement(94.34, 1e-5, "A", "C", params),
                new DistanceMeasurement(94.34, 1e-5, "B", "C", params),
                new LevelingMeasurement(10.0, 1e-3, "A", "C", params)
        );
        Network net = new Network(points, measurements,
                NetworkType.free2D(), 1.0, 3);

        double[] variances = {1e-5, 1e-5, 1e-5, 1e-3};
        SimpleMatrix Q = calculator.computeQ(net, variances);

        int n = Q.numRows();
        assertThat(n).isEqualTo(9);

        // Симметричность
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                assertThat(Q.get(i, j))
                        .isCloseTo(Q.get(j, i), within(1e-9));
            }
        }
    }

    // ================================================================
    // Вспомогательные
    // ================================================================

    /** Сеть полного ранга: расстояния + нивелирование фиксируют X, Y, Z. */
    private Network fullRankNetwork() {
        List<Point> points = List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0),
                new Point("C", false, 50.0, 80.0, 10.0)
        );
        ParameterSet params = ParameterSet.fromPoints(points);

        List<Measurement> measurements = List.of(
                new DistanceMeasurement(94.34, 1e-5, "A", "C", params),
                new DistanceMeasurement(94.34, 1e-5, "B", "C", params),
                new DistanceMeasurement(100.0, 1e-5, "A", "B", params),
                new LevelingMeasurement(10.0, 1e-3, "A", "C", params)
        );

        return new Network(points, measurements, NetworkType.fixed(), 1.0, 3);
    }

    /** 2D-сеть (Z не определена). */
    private Network simpleTriangulationNetwork() {
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

        return new Network(points, measurements, NetworkType.fixed(), 1.0, 2);
    }
}