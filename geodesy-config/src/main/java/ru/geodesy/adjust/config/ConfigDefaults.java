package ru.geodesy.adjust.config;

/**
 * Значения допусков и параметров приборов по умолчанию.
 * Используются, если YAML не задан.
 * <p>
 * Соответствует ГКИНП-02-033-82 (плановые) и
 * Инструкции по нивелированию I–IV классов (высотные).
 */
public final class ConfigDefaults {

    private ConfigDefaults() {
        throw new AssertionError("Utility class");
    }

    // ============================================================
    // Плановые сети (СКО угла в секундах, относительная ошибка)
    // ============================================================

    public static final double PLAN_CLASS1_ANGLE_SIGMA = 0.7;
    public static final String PLAN_CLASS1_REL_DIST = "1:1000000";

    public static final double PLAN_CLASS2_ANGLE_SIGMA = 1.0;
    public static final String PLAN_CLASS2_REL_DIST = "1:250000";

    public static final double PLAN_CLASS3_ANGLE_SIGMA = 2.0;
    public static final String PLAN_CLASS3_REL_DIST = "1:100000";

    public static final double PLAN_CLASS4_ANGLE_SIGMA = 3.0;
    public static final String PLAN_CLASS4_REL_DIST = "1:25000";

    public static final double PLAN_RAZR1_ANGLE_SIGMA = 5.0;
    public static final String PLAN_RAZR1_REL_DIST = "1:10000";

    public static final double PLAN_RAZR2_ANGLE_SIGMA = 10.0;
    public static final String PLAN_RAZR2_REL_DIST = "1:5000";

    // ============================================================
    // Высотные сети (СКО превышения в мм/км)
    // ============================================================

    public static final double HEIGHT_CLASS1_SIGMA = 0.8;
    public static final double HEIGHT_CLASS2_SIGMA = 2.0;
    public static final double HEIGHT_CLASS3_SIGMA = 5.0;
    public static final double HEIGHT_CLASS4_SIGMA = 10.0;

    // ============================================================
    // Приборы
    // ============================================================

    public static final double TACHEOMETER_1S_ANGLE_SIGMA = 1.0;
    public static final double TACHEOMETER_1S_DIST_MM = 1.0;
    public static final double TACHEOMETER_1S_DIST_PPM = 1.5;

    public static final double TACHEOMETER_3S_ANGLE_SIGMA = 3.0;
    public static final double TACHEOMETER_3S_DIST_MM = 2.0;
    public static final double TACHEOMETER_3S_DIST_PPM = 2.0;

    public static final double TACHEOMETER_5S_ANGLE_SIGMA = 5.0;
    public static final double TACHEOMETER_5S_DIST_MM = 3.0;
    public static final double TACHEOMETER_5S_DIST_PPM = 3.0;

    public static final double LEVEL_H2_SIGMA_KM = 0.7;
    public static final double LEVEL_H3_SIGMA_KM = 1.5;
    public static final double LEVEL_H5_SIGMA_KM = 2.5;
}