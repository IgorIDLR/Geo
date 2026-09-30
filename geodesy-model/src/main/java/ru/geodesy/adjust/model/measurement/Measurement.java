package ru.geodesy.adjust.model.measurement;

/**
 * Скалярное измерение с весом.
 * <p>
 * Предполагается неизменяемым: дисперсия задаётся при создании и
 * не может быть изменена после. Для переопределения весов
 * используется {@code WeightProvider}.
 */
public interface Measurement {

    /** Измеренное значение. */
    double getObserved();

    /** Априорная дисперсия σ². */
    double getVariance();

    /** Вычисляет значение измерения по текущему вектору параметров X. */
    double computeValue(double[] X);

    /** Заполняет массив частных производных ∂f/∂X. */
    void derivatives(double[] X, double[] derivatives);

    /** Идентификаторы точек, участвующих в измерении. */
    String[] involvedPointIds();

    /** Класс точности (может быть {@code null}). */
    default String accuracyClass() { return null; }

    /** Имя прибора (может быть {@code null}). */
    default String instrument() { return null; }

    /** Пользовательский тег для группировки. */
    default String tag() { return null; }

    /** Признак, что измерение содержит коррелированные компоненты. */
    default boolean isBlock() { return false; }
}