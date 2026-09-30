package ru.geodesy.adjust.session.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Патч измерения: добавить, изменить или удалить.
 * Позволяет менять отдельные измерения без полного пересчёта сети.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "op")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MeasurementPatch.Add.class, name = "ADD"),
        @JsonSubTypes.Type(value = MeasurementPatch.Update.class, name = "UPDATE"),
        @JsonSubTypes.Type(value = MeasurementPatch.Remove.class, name = "REMOVE")
})
public sealed interface MeasurementPatch {

    /** Добавить новое измерение. */
    record Add(String measurementJson) implements MeasurementPatch {}

    /** Изменить параметры существующего измерения (по индексу). */
    record Update(int index, String measurementJson) implements MeasurementPatch {}

    /** Удалить измерение по индексу. */
    record Remove(int index) implements MeasurementPatch {}
}