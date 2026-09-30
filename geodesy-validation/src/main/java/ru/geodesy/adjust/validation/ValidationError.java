package ru.geodesy.adjust.validation;

/**
 * Одна ошибка валидации с указанием категории, места и сообщения.
 */
public record ValidationError(
        Category category,
        String location,
        String message
) {

    public enum Category {
        SYNTAX,
        SEMANTIC,
        TOPOLOGY,
        GEOMETRY
    }

    public static ValidationError syntax(String location, String message) {
        return new ValidationError(Category.SYNTAX, location, message);
    }

    public static ValidationError semantic(String location, String message) {
        return new ValidationError(Category.SEMANTIC, location, message);
    }

    public static ValidationError topology(String location, String message) {
        return new ValidationError(Category.TOPOLOGY, location, message);
    }

    public static ValidationError geometry(String location, String message) {
        return new ValidationError(Category.GEOMETRY, location, message);
    }

    @Override
    public String toString() {
        return "[" + category + "] " + location + ": " + message;
    }
}