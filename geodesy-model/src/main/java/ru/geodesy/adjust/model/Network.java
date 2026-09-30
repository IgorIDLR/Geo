package ru.geodesy.adjust.model;

import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.common.Preconditions;

import java.util.Collections;
import java.util.List;

/**
 * Геодезическая сеть — неизменяемое описание топологии и измерений.
 * <p>
 * Точки (внутри — мутабельные) хранятся как есть, чтобы
 * {@code ApproximationService} мог заполнять приближённые координаты.
 */
public record Network(
        List<Point> points,
        List<Measurement> measurements,
        NetworkType networkType,
        double sigma0,
        int dimension
) {

    public Network {
        Preconditions.requireNonEmpty(points, "network.points");
        Preconditions.requireNonEmpty(measurements, "network.measurements");
        Preconditions.requirePositive(sigma0, "network.sigma0");
        Preconditions.requireInRange(dimension, 1, 3, "network.dimension");

        // Защитные копии
        points = List.copyOf(points);
        measurements = List.copyOf(measurements);
    }

    // ============================================================
    // Удобные геттеры
    // ============================================================

    /**
     * Возвращает точку по идентификатору или {@code null}.
     */
    public Point findPoint(String id) {
        for (Point p : points) {
            if (p.id().equals(id)) return p;
        }
        return null;
    }

    /**
     * Число определяемых точек.
     */
    public long countFreePoints() {
        return points.stream().filter(p -> !p.fixed()).count();
    }

    /**
     * Число фиксированных точек.
     */
    public long countFixedPoints() {
        return points.stream().filter(Point::fixed).count();
    }

    /**
     * Создаёт новый объект с изменённым списком измерений.
     */
    public Network withMeasurements(List<Measurement> newMeasurements) {
        return new Network(points, newMeasurements, networkType, sigma0, dimension);
    }

    /**
     * Создаёт новый объект с изменённым типом сети.
     */
    public Network withNetworkType(NetworkType type) {
        return new Network(points, measurements, type, sigma0, dimension);
    }
}