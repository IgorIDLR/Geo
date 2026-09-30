package ru.geodesy.adjust.validation.topological;

import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.adjust.validation.ValidationError;
import ru.geodesy.adjust.validation.ValidationResult;

import java.util.*;

/**
 * Детектор изолированных точек — точек, не связанных ни одним измерением.
 * Изолированные точки делают систему вырожденной.
 */
public class IsolatedPointDetector {

    public void detect(Network network, ValidationResult result) {
        Set<String> connected = new HashSet<>();
        for (Measurement m : network.measurements()) {
            Collections.addAll(connected, m.involvedPointIds());
        }

        for (Point p : network.points()) {
            if (!connected.contains(p.id())) {
                result.addError(ValidationError.topology(
                        "point[" + p.id() + "]",
                        "Изолированная точка: не участвует ни в одном измерении"));
            }
        }
    }
}