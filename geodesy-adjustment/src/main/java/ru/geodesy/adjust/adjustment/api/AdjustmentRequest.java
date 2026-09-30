package ru.geodesy.adjust.adjustment.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.geodesy.common.Preconditions;

/**
 * DTO одного запроса на уравнивание геодезической сети.
 * <p>
 * Содержит:
 * <ul>
 *   <li>Метод уравнивания</li>
 *   <li>Исходные данные сети (JSON-строка)</li>
 *   <li>Параметры сходимости</li>
 *   <li>Класс точности и прибор для назначения весов</li>
 *   <li>Опции вывода результата</li>
 * </ul>
 * <p>
 * <b>Immutable</b>: все поля final, конструктор применяет значения
 * по умолчанию, если они не заданы. Массивы копируются.
 */
public record AdjustmentRequest(
        String method,
        String rawData,
        double convergenceEps,
        int maxIterations,
        String networkClass,
        String instrumentName,
        OutputOptions outputOptions
) {

    /**
     * Канонический конструктор с дефолтами и валидацией.
     */
    @JsonCreator
    public AdjustmentRequest(
            @JsonProperty("method") String method,
            @JsonProperty("rawData") String rawData,
            @JsonProperty("convergenceEps") Double convergenceEps,
            @JsonProperty("maxIterations") Integer maxIterations,
            @JsonProperty("networkClass") String networkClass,
            @JsonProperty("instrumentName") String instrumentName,
            @JsonProperty("outputOptions") OutputOptions outputOptions) {

        this(method != null ? method : "PARAMETRIC", Preconditions.requireNonBlank(rawData, "adjustment.rawData"), (convergenceEps != null && convergenceEps > 0)
                ? convergenceEps
                : 1e-6, (maxIterations != null && maxIterations > 0)
                ? maxIterations
                : 100, networkClass, instrumentName, outputOptions != null
                ? outputOptions
                : OutputOptions.FULL);
    }

    /**
     * Фабричный метод для типовых запросов.
     */
    public static AdjustmentRequest of(String method, String rawData) {
        return new AdjustmentRequest(method, rawData, null, null,
                null, null, null);
    }

    /**
     * Проверка, задан ли класс точности.
     */
    public boolean hasNetworkClass() {
        return networkClass != null && !networkClass.isBlank();
    }

    /**
     * Проверка, задан ли прибор.
     */
    public boolean hasInstrument() {
        return instrumentName != null && !instrumentName.isBlank();
    }
}