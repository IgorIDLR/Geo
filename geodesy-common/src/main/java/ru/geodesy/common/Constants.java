package ru.geodesy.common;

/**
 * Глобальные константы проекта.
 * Immutable, не инстанцируется.
 */
public final class Constants {

    private Constants() {
        throw new AssertionError("Utility class");
    }

    // ============================================================
    // Геодезические константы
    // ============================================================

    /** Число секунд в одном радиане: 180° × 3600 / π. */
    public static final double RHO_SECONDS = 206264.806247096355;

    /** Число минут в одном радиане. */
    public static final double RHO_MINUTES = 3437.7467707849396;

    /** Средний радиус Земли (м), по IUGG. */
    public static final double EARTH_RADIUS_MEAN = 6_371_000.0;

    /** Большая полуось эллипсоида WGS84 (м). */
    public static final double WGS84_SEMI_MAJOR = 6_378_137.0;

    /** Сжатие эллипсоида WGS84. */
    public static final double WGS84_FLATTENING = 1.0 / 298.257223563;

    /** Большая полуось эллипсоида Красовского (СК-42, м). */
    public static final double SK42_SEMI_MAJOR = 6_378_245.0;

    /** Сжатие эллипсоида Красовского. */
    public static final double SK42_FLATTENING = 1.0 / 298.3;

    // ============================================================
    // Численные пороги
    // ============================================================

    /** Порог, ниже которого значения считаются эквивалентными нулю. */
    public static final double EPSILON_ZERO = 1e-12;

    /** Порог для сравнения координат (м). */
    public static final double EPSILON_COORD = 1e-8;

    /** Порог для определения движения параметра (м). */
    public static final double MOVEMENT_THRESHOLD = 1e-8;

    /** Максимально допустимое число обусловленности матрицы. */
    public static final double MAX_CONDITION_NUMBER = 1e12;

    /** Порог определения вырожденности матрицы. */
    public static final double SINGULAR_THRESHOLD = 1e-15;

    // ============================================================
    // Параметры по умолчанию
    // ============================================================

    /** Точность сходимости по умолчанию. */
    public static final double DEFAULT_CONVERGENCE_EPS = 1e-6;

    /** Максимальное число итераций по умолчанию. */
    public static final int DEFAULT_MAX_ITERATIONS = 100;

    /** Априорная СКО единицы веса по умолчанию. */
    public static final double DEFAULT_SIGMA0 = 1.0;

    /** Уровень значимости для статистических тестов по умолчанию. */
    public static final double DEFAULT_ALPHA = 0.001;

    /** Мощность теста по умолчанию. */
    public static final double DEFAULT_BETA = 0.80;

    // ============================================================
    // Константы размерности
    // ============================================================

    /** Количество координат на точку (X, Y, Z). */
    public static final int COORDS_PER_POINT = 3;

    /** Индекс координаты X. */
    public static final int IDX_X = 0;

    /** Индекс координаты Y. */
    public static final int IDX_Y = 1;

    /** Индекс координаты Z. */
    public static final int IDX_Z = 2;
}