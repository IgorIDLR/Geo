package ru.geodesy.adjust.validation.topological;

import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.adjust.validation.ValidationError;
import ru.geodesy.adjust.validation.ValidationResult;

import java.util.*;

/**
 * Детектор дублирующихся измерений.
 * <p>
 * Дубликатом считается измерение того же типа, связывающее те же точки
 * (с точностью до перестановки).
 */
public class DuplicateDetector {

    public void detect(List<Measurement> measurements, ValidationResult result) {
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < measurements.size(); i++) {
            Measurement m = measurements.get(i);
            String key = fingerprint(m);
            Integer firstIdx = seen.get(key);
            if (firstIdx != null) {
                result.addError(ValidationError.topology(
                        "measurement[" + i + "]",
                        "Дублирует измерение #" + firstIdx + ": " + key));
            } else {
                seen.put(key, i);
            }
        }
    }

    /**
     * Создаёт «отпечаток» измерения — тип + отсортированные точки.
     */
    private String fingerprint(Measurement m) {
        String[] ids = m.involvedPointIds().clone();
        // Для "from→to" нормализуем порядок
        if (m instanceof ru.geodesy.adjust.model.measurement.DistanceMeasurement
                || m instanceof ru.geodesy.adjust.model.measurement.LevelingMeasurement
                || m instanceof ru.geodesy.adjust.model.measurement.AzimuthMeasurement) {
            Arrays.sort(ids);
        }
        return m.getClass().getSimpleName() + ":" + String.join("-", ids);
    }
}