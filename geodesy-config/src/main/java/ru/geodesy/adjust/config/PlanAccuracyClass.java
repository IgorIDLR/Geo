package ru.geodesy.adjust.config;

import ru.geodesy.common.ValidationException;

/**
 * Классы точности плановых геодезических сетей.
 * <p>
 * Соответствует ГКИНП-02-033-82 и СП 47.13330.2012:
 * <ul>
 *   <li>ГГС 1–4 классы</li>
 *   <li>Разряды 1 и 2</li>
 * </ul>
 */
public enum PlanAccuracyClass {

    /** 1 класс ГГС. */
    CLASS1("class1"),
    /** 2 класс ГГС. */
    CLASS2("class2"),
    /** 3 класс ГГС. */
    CLASS3("class3"),
    /** 4 класс ГГС. */
    CLASS4("class4"),
    /** 1 разряд. */
    RAZR1("razr1"),
    /** 2 разряд. */
    RAZR2("razr2");

    private final String code;

    PlanAccuracyClass(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * Разбирает код из конфигурации.
     */
    public static PlanAccuracyClass fromCode(String code) {
        if (code == null) {
            throw new ValidationException("Код класса точности плановой сети не может быть null");
        }
        String normalized = code.toLowerCase().trim();
        for (PlanAccuracyClass c : values()) {
            if (c.code.equals(normalized)) {
                return c;
            }
        }
        throw new ValidationException("Неизвестный класс точности плановой сети: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}