package ru.geodesy.adjust.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import ru.geodesy.common.ValidationException;

import java.util.Map;

/**
 * Регламентные допуски классов точности.
 * Загружается из {@code application.yml} с префиксом {@code geodesy.tolerances}.
 * <p>
 * Пример конфигурации:
 * <pre>
 * geodesy:
 *   tolerances:
 *     plan:
 *       class1: { angle-sigma: 0.7, relative-dist: "1:1000000" }
 *       class2: { angle-sigma: 1.0, relative-dist: "1:250000" }
 *     height:
 *       class1: { height-sigma: 0.8 }
 *       class2: { height-sigma: 2.0 }
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "geodesy.tolerances")
@Validated
@Data
public class GeodeticTolerances {

    /** Допуски плановых классов точности (ключ — код класса). */
    @NotNull
    private Map<String, PlanTolerance> plan;

    /** Допуски высотных классов точности. */
    @NotNull
    private Map<String, HeightTolerance> height;

    // ============================================================
    // Вложенные DTO
    // ============================================================

    /**
     * Допуски плановой сети.
     */
    @Data
    public static class PlanTolerance {

        /** СКО измерения угла (секунды дуги). */
        @Positive
        private double angleSigma;

        /** Относительная ошибка расстояния в формате "1:N". */
        private String relativeDist;

        /**
         * Возвращает знаменатель относительной ошибки (N).
         *
         * @throws ValidationException если формат неверный
         */
        public double relativeDistDenominator() {
            if (relativeDist == null || relativeDist.isBlank()) {
                throw new ValidationException(
                        "Относительная ошибка не задана для планового класса");
            }
            int colon = relativeDist.indexOf(':');
            if (colon < 0) {
                throw new ValidationException(
                        "Неверный формат относительной ошибки: " + relativeDist);
            }
            String denominator = relativeDist.substring(colon + 1).trim();
            try {
                return Double.parseDouble(denominator);
            } catch (NumberFormatException e) {
                throw new ValidationException(
                        "Не удалось разобрать знаменатель: " + denominator, e);
            }
        }
    }

    /**
     * Допуски высотной сети.
     */
    @Data
    public static class HeightTolerance {

        /** СКО превышения (мм на 1 км хода). */
        @Positive
        private double heightSigma;
    }

    // ============================================================
    // Удобные методы доступа
    // ============================================================

    /**
     * Возвращает допуск планового класса.
     *
     * @throws ValidationException если класс не найден
     */
    public PlanTolerance requirePlan(String code) {
        PlanAccuracyClass cls = PlanAccuracyClass.fromCode(code);
        PlanTolerance t = plan.get(cls.code());
        if (t == null) {
            throw new ValidationException(
                    "Допуск для планового класса '" + code + "' не сконфигурирован");
        }
        return t;
    }

    /**
     * Возвращает допуск высотного класса.
     */
    public HeightTolerance requireHeight(String code) {
        HeightAccuracyClass cls = HeightAccuracyClass.fromCode(code);
        HeightTolerance t = height.get(cls.code());
        if (t == null) {
            throw new ValidationException(
                    "Допуск для высотного класса '" + code + "' не сконфигурирован");
        }
        return t;
    }

    /**
     * Возвращает допуск планового класса или {@code null}.
     */
    public PlanTolerance getPlanOrNull(String code) {
        if (code == null) return null;
        try {
            return plan.get(PlanAccuracyClass.fromCode(code).code());
        } catch (ValidationException e) {
            return null;
        }
    }

    /**
     * Возвращает допуск высотного класса или {@code null}.
     */
    public HeightTolerance getHeightOrNull(String code) {
        if (code == null) return null;
        try {
            return height.get(HeightAccuracyClass.fromCode(code).code());
        } catch (ValidationException e) {
            return null;
        }
    }
}