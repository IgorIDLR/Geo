package ru.geodesy.adjust.config;

import ru.geodesy.common.ValidationException;

/**
 * Классы точности высотных (нивелирных) сетей.
 * <p>
 * Соответствует ГОСТ 8.016-2018 и Инструкции по нивелированию I–IV классов.
 */
public enum HeightAccuracyClass {

    /** I класс. */
    CLASS1("class1"),
    /** II класс. */
    CLASS2("class2"),
    /** III класс. */
    CLASS3("class3"),
    /** IV класс. */
    CLASS4("class4");

    private final String code;

    HeightAccuracyClass(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static HeightAccuracyClass fromCode(String code) {
        if (code == null) {
            throw new ValidationException("Код класса точности высотной сети не может быть null");
        }
        String normalized = code.toLowerCase().trim();
        for (HeightAccuracyClass c : values()) {
            if (c.code.equals(normalized)) {
                return c;
            }
        }
        throw new ValidationException("Неизвестный класс точности высотной сети: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}