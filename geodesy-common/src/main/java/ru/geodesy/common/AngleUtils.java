package ru.geodesy.common;

/**
 * Утилиты для работы с углами: преобразования между радианами,
 * градусами, DMS-форматом.
 */
public final class AngleUtils {

    private AngleUtils() {
        throw new AssertionError("Utility class");
    }

    /**
     * Переводит градусы в радианы.
     */
    public static double degreesToRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    /**
     * Переводит радианы в градусы.
     */
    public static double radiansToDegrees(double radians) {
        return Math.toDegrees(radians);
    }

    /**
     * Переводит угловые секунды в радианы.
     */
    public static double secondsToRadians(double seconds) {
        return seconds / Constants.RHO_SECONDS;
    }

    /**
     * Переводит радианы в угловые секунды.
     */
    public static double radiansToSeconds(double radians) {
        return radians * Constants.RHO_SECONDS;
    }

    /**
     * Переводит DMS в десятичные градусы.
     * Знак берётся от градусов.
     */
    public static double dmsToDegrees(double d, double m, double s) {
        double sign = d < 0 ? -1 : 1;
        return sign * (Math.abs(d) + m / 60.0 + s / 3600.0);
    }

    /**
     * Переводит DMS в радианы.
     */
    public static double dmsToRadians(double d, double m, double s) {
        return Math.toRadians(dmsToDegrees(d, m, s));
    }

    /**
     * Разбирает строку DMS вида "57°17′44.8″" или "57 17 44.8".
     * Возвращает радианы.
     */
    public static double parseDmsToRadians(String dms) {
        if (dms == null || dms.isBlank()) {
            throw new ValidationException("Пустая строка DMS");
        }
        String cleaned = dms
                .replace("°", " ")
                .replace("′", " ")
                .replace("″", " ")
                .replace(",", ".")
                .trim();
        String[] parts = cleaned.split("\\s+");
        try {
            double d = parts.length > 0 ? Double.parseDouble(parts[0]) : 0;
            double m = parts.length > 1 ? Double.parseDouble(parts[1]) : 0;
            double s = parts.length > 2 ? Double.parseDouble(parts[2]) : 0;
            return dmsToRadians(d, m, s);
        } catch (NumberFormatException e) {
            throw new ValidationException("Не удалось разобрать DMS: " + dms, e);
        }
    }

    /**
     * Форматирует радианы в строку DMS.
     */
    public static String radiansToDmsString(double radians) {
        double degrees = Math.toDegrees(radians);
        int sign = degrees < 0 ? -1 : 1;
        degrees = Math.abs(degrees);

        int d = (int) degrees;
        double minFloat = (degrees - d) * 60;
        int m = (int) minFloat;
        double s = (minFloat - m) * 60;

        return String.format("%s%d°%02d′%05.2f″",
                sign < 0 ? "-" : "", d, m, s);
    }

    /**
     * Приводит угол к диапазону [0, 2π).
     */
    public static double normalizeTo0_2Pi(double radians) {
        double result = radians % (2 * Math.PI);
        return result < 0 ? result + 2 * Math.PI : result;
    }

    /**
     * Приводит угол к диапазону (−π, π].
     */
    public static double normalizeToPi(double radians) {
        double result = radians % (2 * Math.PI);
        if (result <= -Math.PI) result += 2 * Math.PI;
        if (result > Math.PI) result -= 2 * Math.PI;
        return result;
    }

    /**
     * Дирекционный угол между двумя точками.
     * Возвращает азимут из (x1, y1) в (x2, y2) в радианах, [0, 2π).
     */
    public static double azimuth(double x1, double y1, double x2, double y2) {
        return normalizeTo0_2Pi(Math.atan2(y2 - y1, x2 - x1));
    }
}