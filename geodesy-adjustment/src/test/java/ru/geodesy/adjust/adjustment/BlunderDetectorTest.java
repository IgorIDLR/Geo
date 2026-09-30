package ru.geodesy.adjust.adjustment;

import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.adjustment.internal.statistics.BlunderDetector;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.NetworkType;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.*;

import java.awt.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BlunderDetectorTest {

    private final BlunderDetector detector = new BlunderDetector();

    @Test
    void detectsLargeBlunder() {
        // Нивелирный ход: 4 точки, 5 превышений. Одно с ошибкой 10σ.
        List<Point> points = List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", false, 0.0, 0.0, 10.0),
                new Point("C", false, 0.0, 0.0, 20.0),
                new Point("D", false, 0.0, 0.0, 30.0)
        );
        ParameterSet params = ParameterSet.fromPoints(points);

        List<Measurement> measurements = List.of(
                new LevelingMeasurement(10.0, 1.0, "A", "B", params),
                new LevelingMeasurement(10.0, 1.0, "B", "C", params),
                new LevelingMeasurement(10.0, 1.0, "C", "D", params),
                new LevelingMeasurement(-5.0, 1.0, "A", "D", params),  // выброс
                new LevelingMeasurement(30.0, 1.0, "A", "D", params)    // дублирующее
        );
        Network net = new Network(points, measurements, NetworkType.fixed(), 1.0, 1);

        // Задаём приближённые координаты
        double[] X = {0, 0, 0, 0, 0, 10, 0, 0, 20, 0, 0, 30};

        // Простой MNE-результат: Q = I (для теста)
        SimpleMatrix Q = SimpleMatrix.identity(12).scale(1e-3);

        var blunders = detector.detect(net, params, X, Q, 1.0, 0.001, 0.80);
        assertThat(blunders).isNotEmpty();
    }
}