package ru.geodesy.adjust.model;

import ru.geodesy.common.Preconditions;

import java.util.Objects;

/**
 * Геодезический пункт.
 * <p>
 * Мутабельный по координатам (X, Y, Z), так как {@code ApproximationService}
 * заполняет приближённые значения для определяемых точек. Идентификатор
 * и флаг {@code fixed} неизменяемы.
 * <p>
 * Ковариационная матрица координат (если задана) используется для учёта
 * ошибок исходных данных при уравнивании.
 */
public class Point {

    private final String id;
    private boolean fixed;
    private Double x;
    private Double y;
    private Double z;
    private double[][] covariance;

    public Point(String id, boolean fixed, Double x, Double y, Double z) {
        this(id, fixed, x, y, z, null);
    }

    public Point(String id, boolean fixed, Double x, Double y, Double z,
                 double[][] covariance) {
        this.id = Preconditions.requireNonBlank(id, "point.id");
        this.fixed = fixed;
        this.x = x;
        this.y = y;
        this.z = z;
        this.covariance = validateCovariance(covariance);
    }

    // ============================================================
    // Геттеры
    // ============================================================

    public String id() { return id; }
    public boolean fixed() { return fixed; }
    public Double x() { return x; }
    public Double y() { return y; }
    public Double z() { return z; }
    public double[][] covariance() { return covariance; }

    /**
     * Проверяет наличие всех трёх координат.
     */
    public boolean hasCoordinates() {
        return x != null && y != null && z != null;
    }

    /**
     * Проверяет наличие плановых координат (X, Y).
     */
    public boolean hasPlanarCoordinates() {
        return x != null && y != null;
    }

    /**
     * Проверяет, задана ли ковариационная матрица.
     */
    public boolean hasCovariance() {
        return covariance != null;
    }

    // ============================================================
    // Сеттеры (для приближений и трансформаций)
    // ============================================================

    public void setX(Double x) { this.x = x; }
    public void setY(Double y) { this.y = y; }
    public void setZ(Double z) { this.z = z; }
    public void setFixed(boolean fixed) { this.fixed = fixed; }
    public void setCovariance(double[][] covariance) {
        this.covariance = validateCovariance(covariance);
    }

    /**
     * Установка всех координат одной операцией.
     */
    public void setCoordinates(Double x, Double y, Double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // ============================================================
    // equals / hashCode / toString — по id
    // ============================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Point{" + id
                + (fixed ? " [fixed]" : "")
                + ", x=" + x + ", y=" + y + ", z=" + z
                + (covariance != null ? ", cov=3×3" : "")
                + "}";
    }

    // ============================================================
    // Приватные утилиты
    // ============================================================

    private static double[][] validateCovariance(double[][] cov) {
        if (cov == null) return null;
        if (cov.length != 3 || cov[0].length != 3) {
            throw new IllegalArgumentException(
                    "Ковариационная матрица должна быть 3×3");
        }
        // Проверка симметричности
        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 3; j++) {
                if (Math.abs(cov[i][j] - cov[j][i]) > 1e-12) {
                    throw new IllegalArgumentException(
                            "Ковариационная матрица несимметрична");
                }
            }
            if (cov[i][i] < 0) {
                throw new IllegalArgumentException(
                        "Диагональный элемент ковариации отрицателен");
            }
        }
        return cov;
    }
}