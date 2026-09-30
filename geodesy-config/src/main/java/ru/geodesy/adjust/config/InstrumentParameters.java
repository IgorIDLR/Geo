package ru.geodesy.adjust.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import ru.geodesy.common.ValidationException;

import java.util.Map;

/**
 * Паспортные параметры геодезических приборов.
 * Загружается с префиксом {@code geodesy.instruments}.
 * <p>
 * Пример:
 * <pre>
 * geodesy:
 *   instruments:
 *     props:
 *       tacheometer-1s: { angle-sigma: 1.0, distance-sigma-mm: 1.0, distance-sigma-ppm: 1.5 }
 *       level-h2: { height-sigma-km: 0.7 }
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "geodesy.instruments")
@Validated
@Data
public class InstrumentParameters {

    @NotNull
    private Map<String, InstrumentProps> props;

    @Data
    public static class InstrumentProps {

        /** СКО измерения угла (секунды дуги). */
        private Double angleSigma;

        /** Постоянная часть СКО расстояния (мм). */
        private Double distanceSigmaMm;

        /** Пропорциональная часть СКО расстояния (ppm). */
        private Double distanceSigmaPpm;

        /** СКО превышения (мм/км). */
        private Double heightSigmaKm;

        /** СКО планового положения GNSS (мм). */
        private Double horizontalSigmaMm;

        /** Пропорциональная СКО GNSS по плану (ppm). */
        private Double horizontalSigmaPpm;

        /** СКО высотного положения GNSS (мм). */
        private Double verticalSigmaMm;

        /** Пропорциональная СКО GNSS по высоте (ppm). */
        private Double verticalSigmaPpm;

        // ============================================================
        // Классификация типа прибора
        // ============================================================

        /** Точность по углу задана (это тахеометр/теодолит). */
        public boolean isAngleCapable() {
            return angleSigma != null;
        }

        /** Точность по расстоянию задана. */
        public boolean isDistanceCapable() {
            return distanceSigmaMm != null || distanceSigmaPpm != null;
        }

        /** Точность по превышению задана (нивелир). */
        public boolean isLevelCapable() {
            return heightSigmaKm != null;
        }

        /** Точность GNSS задана. */
        public boolean isGnssCapable() {
            return horizontalSigmaMm != null || verticalSigmaMm != null;
        }
    }

    // ============================================================
    // Удобные методы доступа
    // ============================================================

    /**
     * Возвращает параметры прибора или выбрасывает исключение.
     */
    public InstrumentProps require(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Имя прибора не задано");
        }
        InstrumentProps p = props.get(name);
        if (p == null) {
            throw new ValidationException("Прибор не найден в конфигурации: " + name);
        }
        return p;
    }

    /**
     * Возвращает параметры прибора или {@code null}.
     */
    public InstrumentProps getOrNull(String name) {
        if (name == null || name.isBlank()) return null;
        return props.get(name);
    }

    /**
     * Проверяет, есть ли прибор в конфигурации.
     */
    public boolean contains(String name) {
        return name != null && props.containsKey(name);
    }
}