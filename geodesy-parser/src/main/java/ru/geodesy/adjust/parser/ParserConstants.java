package ru.geodesy.adjust.parser;

/**
 * Константы модуля парсинга.
 */
public final class ParserConstants {

    private ParserConstants() {
        throw new AssertionError("Utility class");
    }

    /** Разделитель для DMS-строки. */
    public static final String DMS_SEPARATOR_REGEX = "\\s+";

    /** Символ градуса. */
    public static final String DEGREE_SYMBOL = "°";

    /** Символ минуты. */
    public static final String MINUTE_SYMBOL = "′";

    /** Символ секунды. */
    public static final String SECOND_SYMBOL = "″";

    /** Ключ JSON-объекта для описания сети. */
    public static final String KEY_POINTS = "points";

    /** Ключ JSON-объекта для списка измерений. */
    public static final String KEY_MEASUREMENTS = "measurements";

    /** Ключ JSON-объекта для типа сети. */
    public static final String KEY_NETWORK_TYPE = "networkType";

    /** Ключ JSON-объекта для априорной СКО единицы веса. */
    public static final String KEY_SIGMA0 = "sigma0";

    /** Ключ JSON-объекта для размерности сети. */
    public static final String KEY_DIMENSION = "dimension";

    /** Тип узла для упрощённого формата описания трассы. */
    public static final String KEY_TYPE = "type";

    /** Значение типа "TRAVERSE". */
    public static final String VALUE_TRAVERSE = "TRAVERSE";
}