package ru.geodesy.common;

/**
 * Утилиты для численных вычислений.
 * Immutable, не инстанцируется.
 */
public final class MathUtils {

    private MathUtils() {
        throw new AssertionError("Utility class");
    }

    /**
     * Безопасное вычисление квадратного корня с защитой от
     * отрицательных значений (возвращает 0).
     */
    public static double safeSqrt(double value) {
        return value > 0 ? Math.sqrt(value) : 0.0;
    }

    /**
     * Квадрат числа.
     */
    public static double square(double x) {
        return x * x;
    }

    /**
     * Ограничивает значение диапазоном.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Возвращает число, ограниченное снизу минимальным значением.
     * Полезно для защиты от нулевых/отрицательных дисперсий.
     */
    public static double atLeast(double value, double min) {
        return Math.max(value, min);
    }

    /**
     * Проверяет, равны ли два числа в пределах заданной точности.
     */
    public static boolean nearlyEqual(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    /**
     * Проверяет, равно ли число нулю в пределах {@link Constants#EPSILON_ZERO}.
     */
    public static boolean isZero(double value) {
        return Math.abs(value) < Constants.EPSILON_ZERO;
    }

    /**
     * Проверяет, является ли число конечным (не NaN, не Infinity).
     */
    public static boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    /**
     * Квантиль стандартного нормального распределения.
     * Используется рациональная аппроксимация Acklam (точность ~1e-15).
     */
    public static double normalQuantile(double p) {
        if (p <= 0 || p >= 1) {
            throw new ValidationException("Вероятность должна быть в (0, 1): " + p);
        }

        final double[] a = {
                -3.969683028665376e+01,
                2.209460984245205e+02,
                -2.759285104469687e+02,
                1.383577518672690e+02,
                -3.066479806614716e+01,
                2.506628277459239e+00
        };
        final double[] b = {
                -5.447609879822406e+01,
                1.615858368580409e+02,
                -1.556989798598866e+02,
                6.680131188771972e+01,
                -1.328068155288572e+01
        };
        final double[] c = {
                -7.784894002430293e-03,
                -3.223964580411365e-01,
                -2.400758277161838e+00,
                -2.549732539343734e+00,
                4.374664141464968e+00,
                2.938163982698783e+00
        };
        final double[] d = {
                7.784695709041462e-03,
                3.224671290700398e-01,
                2.445134137142996e+00,
                3.754408661907416e+00
        };

        final double pLow = 0.02425;
        final double pHigh = 1 - pLow;

        if (p < pLow) {
            double q = Math.sqrt(-2 * Math.log(p));
            return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
        } else if (p <= pHigh) {
            double q = p - 0.5;
            double r = q * q;
            return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
                    (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1);
        } else {
            double q = Math.sqrt(-2 * Math.log(1 - p));
            return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
        }
    }

    /**
     * Медиана массива. Изменяет входной массив (сортирует).
     */
    public static double medianInPlace(double[] values) {
        if (values.length == 0) return Double.NaN;
        java.util.Arrays.sort(values);
        int n = values.length;
        if (n % 2 == 0) {
            return 0.5 * (values[n / 2 - 1] + values[n / 2]);
        }
        return values[n / 2];
    }

    /**
     * Медиана массива без модификации исходного.
     */
    public static double median(double[] values) {
        return medianInPlace(values.clone());
    }

    /**
     * MAD — median absolute deviation.
     * Постоянная 1.4826 приводит к согласованности с σ для нормального
     * распределения.
     */
    public static double mad(double[] values) {
        if (values.length == 0) return Double.NaN;
        double med = median(values);
        double[] deviations = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            deviations[i] = Math.abs(values[i] - med);
        }
        return 1.4826 * medianInPlace(deviations);
    }
}