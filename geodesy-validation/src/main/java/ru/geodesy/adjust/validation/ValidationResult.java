package ru.geodesy.adjust.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Результат валидации: список ошибок и предупреждений.
 * Потокобезопасное накопление.
 */
public final class ValidationResult {

    private final List<ValidationError> errors = Collections.synchronizedList(new ArrayList<>());
    private final List<String> warnings = Collections.synchronizedList(new ArrayList<>());

    public void addError(ValidationError error) {
        errors.add(error);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<ValidationError> errors() {
        return List.copyOf(errors);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    /**
     * Объединяет другой результат в текущий.
     */
    public void merge(ValidationResult other) {
        errors.addAll(other.errors);
        warnings.addAll(other.warnings);
    }

    /**
     * Возвращает форматированную строку со всеми ошибками.
     */
    public String describeErrors() {
        if (errors.isEmpty()) {
            return "Ошибок нет";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Обнаружено ").append(errors.size()).append(" ошибок:\n");
        for (ValidationError e : errors) {
            sb.append("  • ").append(e).append("\n");
        }
        return sb.toString();
    }

    /**
     * Возвращает первые N ошибок для краткого сообщения.
     */
    public String describeErrorsShort(int max) {
        if (errors.isEmpty()) {
            return "Ошибок нет";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (ValidationError e : errors) {
            if (count >= max) {
                sb.append("  ... и ещё ").append(errors.size() - max).append(" ошибок\n");
                break;
            }
            sb.append("  • ").append(e).append("\n");
            count++;
        }
        return sb.toString();
    }
}