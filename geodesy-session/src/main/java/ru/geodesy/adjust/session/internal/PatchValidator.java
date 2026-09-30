package ru.geodesy.adjust.session.internal;

import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.session.api.MeasurementPatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Проверка патчей на корректность.
 */
@Component
class PatchValidator {

    List<String> validate(Network network, List<MeasurementPatch> patches) {
        List<String> errors = new ArrayList<>();
        int size = network.measurements().size();
        boolean[] removed = new boolean[size];

        for (int i = 0; i < patches.size(); i++) {
            MeasurementPatch p = patches.get(i);
            switch (p) {
                case MeasurementPatch.Add a -> {
                    if (a.measurementJson() == null || a.measurementJson().isBlank()) {
                        errors.add("Патч #" + i + ": пустое измерение");
                    }
                }
                case MeasurementPatch.Update u -> {
                    if (u.index() < 0 || u.index() >= size) {
                        errors.add("Патч #" + i + ": индекс " + u.index()
                                + " вне диапазона [0, " + size + ")");
                    }
                    if (removed[u.index()]) {
                        errors.add("Патч #" + i + ": попытка изменить удалённое измерение");
                    }
                }
                case MeasurementPatch.Remove r -> {
                    if (r.index() < 0 || r.index() >= size) {
                        errors.add("Патч #" + i + ": индекс " + r.index()
                                + " вне диапазона");
                    }
                    if (removed[r.index()]) {
                        errors.add("Патч #" + i + ": повторное удаление индекса " + r.index());
                    }
                    removed[r.index()] = true;
                }
            }
        }
        return errors;
    }
}