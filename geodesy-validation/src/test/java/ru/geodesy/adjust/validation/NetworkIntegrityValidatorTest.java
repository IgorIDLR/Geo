package ru.geodesy.adjust.validation;

import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.model.*;
import ru.geodesy.adjust.model.measurement.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkIntegrityValidatorTest {

    private final NetworkIntegrityValidator validator = new NetworkIntegrityValidator();

    @Test
    void validNetworkPasses() {
        List<Point> points = List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0),
                new Point("C", false, null, null, null)
        );
        ParameterSet params = ParameterSet.fromPoints(points);
        List<Measurement> measurements = List.of(
                new DistanceMeasurement(100.0, 1e-5, "A", "C", params),
                new DistanceMeasurement(100.0, 1e-5, "B", "C", params),
                new DistanceMeasurement(100.0, 1e-5, "A", "B", params)
        );
        Network net = new Network(points, measurements,
                NetworkType.fixed(), 1.0, 2);

        ValidationResult result = validator.validate(net);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void detectsIsolatedPoint() {
        List<Point> points = List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0),
                new Point("C", false, null, null, null),
                new Point("ISOLATED", false, null, null, null)   // изолированная
        );
        ParameterSet params = ParameterSet.fromPoints(points);
        List<Measurement> measurements = List.of(
                new DistanceMeasurement(100.0, 1e-5, "A", "B", params),
                new DistanceMeasurement(100.0, 1e-5, "A", "C", params),
                new DistanceMeasurement(100.0, 1e-5, "B", "C", params)
        );
        Network net = new Network(points, measurements,
                NetworkType.fixed(), 1.0, 2);

        ValidationResult result = validator.validate(net);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors())
                .anyMatch(e -> e.message().contains("Изолированная"));
    }

    @Test
    void detectsDisconnectedGraph() {
        List<Point> points = List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0),
                new Point("C", false, 200.0, 0.0, 0.0),
                new Point("D", false, 300.0, 0.0, 0.0)
        );
        ParameterSet params = ParameterSet.fromPoints(points);
        // Две независимые компоненты: {A,B} и {C,D}
        List<Measurement> measurements = List.of(
                new DistanceMeasurement(100.0, 1e-5, "A", "B", params),
                new DistanceMeasurement(100.0, 1e-5, "C", "D", params)
        );
        Network net = new Network(points, measurements,
                NetworkType.fixed(), 1.0, 2);

        ValidationResult result = validator.validate(net);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors())
                .anyMatch(e -> e.message().contains("не связна")
                        || e.message().contains("Недостижимые"));
    }

    @Test
    void detectsDuplicateMeasurements() {
        List<Point> points = List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0)
        );
        ParameterSet params = ParameterSet.fromPoints(points);
        // A→B и B→A — для расстояния это дубликат
        List<Measurement> measurements = List.of(
                new DistanceMeasurement(100.0, 1e-5, "A", "B", params),
                new DistanceMeasurement(100.0, 1e-5, "B", "A", params)
        );
        Network net = new Network(points, measurements,
                NetworkType.fixed(), 1.0, 2);

        ValidationResult result = validator.validate(net);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors())
                .anyMatch(e -> e.message().contains("Дублирует"));
    }

    @Test
    void detectsInsufficientMeasurements() {
        List<Point> points = List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", false, null, null, null)
        );
        ParameterSet params = ParameterSet.fromPoints(points);
        List<Measurement> measurements = List.of(
                new DistanceMeasurement(100.0, 1e-5, "A", "B", params)
        );
        Network net = new Network(points, measurements,
                NetworkType.fixed(), 1.0, 2);

        ValidationResult result = validator.validate(net);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors())
                .anyMatch(e -> e.message().contains("Недостаточно"));
    }
}