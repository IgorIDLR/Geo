package ru.geodesy.adjust.design;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.design.api.PrecisionRequest;
import ru.geodesy.adjust.design.api.PrecisionResult;
import ru.geodesy.adjust.design.internal.PrecisionAnalysisServiceImpl;
import ru.geodesy.adjust.design.internal.PrecisionCalculator;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.NetworkType;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.DistanceMeasurement;
import ru.geodesy.adjust.model.measurement.Measurement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PrecisionAnalysisServiceTest {

    private PrecisionAnalysisServiceImpl service;

    @BeforeEach
    void setup() {
        PrecisionCalculator calculator = new PrecisionCalculator();
        service = new PrecisionAnalysisServiceImpl(calculator);
    }

    @Test
    void analysisReturnsPositiveStdDevsForAllPoints() {
        Network net = simpleTriangulation();
        double[] variances = {1e-5, 1e-5, 1e-5};

        PrecisionResult result = service.analyze(
                new PrecisionRequest(net, variances, 1.0));

        assertThat(result.stdDevCoordinates()).containsKeys("A", "B", "C");

        for (var entry : result.stdDevCoordinates().entrySet()) {
            double[] std = entry.getValue();
            assertThat(std[0]).isGreaterThanOrEqualTo(0);
            assertThat(std[1]).isGreaterThanOrEqualTo(0);
        }

        assertThat(result.maxStdDev()).isGreaterThan(0);
        assertThat(result.worstPoint()).isNotNull();
        assertThat(result.elapsedMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void moreAccurateMeasurementsGiveBetterPrecision() {
        Network net = simpleTriangulation();

        PrecisionResult low = service.analyze(
                new PrecisionRequest(net, new double[]{1e-8, 1e-8, 1e-8}, 1.0));
        PrecisionResult high = service.analyze(
                new PrecisionRequest(net, new double[]{1e-3, 1e-3, 1e-3}, 1.0));

        // При точных измерениях maxStd меньше
        assertThat(low.maxStdDev()).isLessThan(high.maxStdDev());
    }

    @Test
    void errorEllipsesAreValidForAllPoints() {
        Network net = simpleTriangulation();
        double[] variances = {1e-5, 1e-5, 1e-5};

        PrecisionResult result = service.analyze(
                new PrecisionRequest(net, variances, 1.0));

        assertThat(result.errorEllipses()).containsKeys("A", "B", "C");

        for (var entry : result.errorEllipses().entrySet()) {
            double semiMajor = result.getSemiMajorAxis(entry.getKey());
            double semiMinor = result.getSemiMinorAxis(entry.getKey());
            assertThat(semiMajor).isGreaterThanOrEqualTo(semiMinor);
            assertThat(semiMinor).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void sigma0ScalesStdDevs() {
        Network net = simpleTriangulation();
        double[] variances = {1e-5, 1e-5, 1e-5};

        PrecisionResult sigma0_1 = service.analyze(
                new PrecisionRequest(net, variances, 1.0));
        PrecisionResult sigma0_2 = service.analyze(
                new PrecisionRequest(net, variances, 2.0));

        double ratio = sigma0_2.maxStdDev() / sigma0_1.maxStdDev();
        assertThat(ratio).isCloseTo(2.0, within(1e-6));
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