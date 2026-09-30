package ru.geodesy.adjust.parser;

import ru.geodesy.common.ValidationException;

/**
 * Ошибка разбора входных данных.
 * Наследует {@link ValidationException} для единой обработки на уровне
 * контроллеров.
 */
public class ParserException extends ValidationException {

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}