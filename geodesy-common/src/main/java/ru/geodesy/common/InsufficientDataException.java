package ru.geodesy.common;

import java.io.Serial;
import java.util.List;

/**
 * Недостаточно данных для выполнения расчёта:
 * изолированные точки, нехватка связей, отсутствие эпох.
 */
public final class InsufficientDataException extends GeodesyException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<String> problematicItems;

    public InsufficientDataException(String message, List<String> problematicItems) {
        super(message);
        this.problematicItems = List.copyOf(problematicItems);
    }

    public InsufficientDataException(String message) {
        this(message, List.of());
    }

    public List<String> getProblematicItems() {
        return problematicItems;
    }

    /**
     * Возвращает строку с перечислением проблемных элементов.
     */
    public String describeProblems() {
        if (problematicItems.isEmpty()) {
            return "";
        }
        return "Проблемные элементы: " + String.join(", ", problematicItems);
    }
}