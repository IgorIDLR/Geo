package ru.geodesy.adjust.transformation.internal;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.*;
import org.springframework.stereotype.Component;

/**
 * Определение параметров трансформации по общим точкам.
 * <p>
 * Реализованы три модели преобразования:
 * <ul>
 *   <li>{@link Model#SHIFT_3} — 3 параметра (dx, dy, dz), чистое смещение</li>
 *   <li>{@link Model#HELMERT_7} — 7 параметров Бурса-Вольфа
 *       (dx, dy, dz, rx, ry, rz, ds)</li>
 *   <li>{@link Model#AFFINE_12} — 12 параметров аффинного преобразования
 *       (матрица 3×4)</li>
 * </ul>
 * Параметры определяются методом наименьших квадратов. Поддерживаются
 * как 2D, так и 3D общие точки.
 */
@Slf4j
@Component
public class BasePointTransformation {

    /** Минимальное число общих точек для определения параметров. */
    private static final int MIN_POINTS = 3;

    /** Модель преобразования. */
    public enum Model {
        /** 3 параметра (dx, dy, dz) — чистое смещение. */
        SHIFT_3,
        /** 7 параметров (dx, dy, dz, rx, ry, rz, ds) — Бурса-Вольфа. */
        HELMERT_7,
        /** 12 параметров (матрица 3×4) — аффинное преобразование. */
        AFFINE_12
    }

    /** Результат определения параметров. */
    public record Result(
            Model model,
            double[] parameters,
            double rmsError,
            int iterations
    ) {
        public Result {
            if (model == null) {
                throw new IllegalArgumentException("model не может быть null");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("parameters не может быть null");
            }
        }

        /** Возвращает параметр по индексу. */
        public double param(int i) {
            return parameters[i];
        }
    }

    // ================================================================
    // Публичный API
    // ================================================================

    /**
     * Оценка параметров трансформации по общим точкам.
     *
     * @param fromPoints координаты в исходной системе [n][2 или 3]
     * @param toPoints   координаты в целевой системе [n][2 или 3]
     * @param model      модель преобразования
     * @return параметры и RMS-ошибка
     * @throws IllegalArgumentException при некорректных данных
     */
    public Result estimate(double[][] fromPoints, double[][] toPoints, Model model) {
        if (model == null) {
            throw new IllegalArgumentException("Модель не может быть null");
        }
        validateArrays(fromPoints, toPoints);

        int n = fromPoints.length;
        log.debug("Оценка {} по {} точкам", model, n);

        return switch (model) {
            case SHIFT_3 -> estimateShift(fromPoints, toPoints, n);
            case HELMERT_7 -> estimateHelmert(fromPoints, toPoints, n);
            case AFFINE_12 -> estimateAffine(fromPoints, toPoints, n);
        };
    }

    // ================================================================
    // 3-параметрическое (SHIFT_3)
    // ================================================================

    /**
     * Оценка 3-параметрического сдвига (dx, dy, dz) методом наименьших
     * квадратов через усреднение разностей.
     * <p>
     * Для модели «чистый сдвиг» МНК-решение имеет вид:
     * <pre>
     *   d_k = (1/n) · Σᵢ (to[i][k] − from[i][k]),   k ∈ {x, y, z}
     * </pre>
     * Поддерживает 2D-точки (при этом dz = 0).
     */
    private Result estimateShift(double[][] from, double[][] to, int n) {
        // 1. Определяем размерность (2D или 3D)
        boolean has3D = hasThirdCoordinate(from) && hasThirdCoordinate(to);
        int dims = has3D ? 3 : 2;

        // 2. Собираем разности и валидируем координаты
        double[] shift = new double[dims];
        for (int i = 0; i < n; i++) {
            validatePoint(from[i], dims, "from[" + i + "]");
            validatePoint(to[i], dims, "to[" + i + "]");

            for (int d = 0; d < dims; d++) {
                shift[d] += to[i][d] - from[i][d];
            }
        }

        // 3. Усреднение
        for (int d = 0; d < dims; d++) {
            shift[d] /= n;
        }

        // 4. Формируем полный вектор из 3 параметров (dz = 0 для 2D)
        double[] params = new double[]{
                shift[0],
                shift[1],
                has3D ? shift[2] : 0.0
        };

        // 5. RMS
        double rms = computeRms(from, to, p -> new double[]{
                p[0] + params[0],
                p[1] + params[1],
                (p.length > 2 ? p[2] : 0.0) + params[2]
        });

        log.info("SHIFT_3: dx = {}, dy = {}, dz = {}, RMS = {} ({}-D, n = {})",
                params[0], params[1], params[2], rms, dims, n);

        return new Result(Model.SHIFT_3, params, rms, 1);
    }

    // ================================================================
    // 7-параметрическое (HELMERT_7)
    // ================================================================

    /**
     * Оценка 7-параметрического преобразования Бурса-Вольфа
     * через линеаризацию относительно центра тяжести.
     */
    private Result estimateHelmert(double[][] from, double[][] to, int n) {
        // Валидация 3D-точек
        for (int i = 0; i < n; i++) {
            validatePoint(from[i], 3, "from[" + i + "]");
            validatePoint(to[i], 3, "to[" + i + "]");
        }

        double[] fromCentroid = centroid(from, n);
        double[] toCentroid = centroid(to, n);

        // Линеаризованная система: L = A · Δp
        RealMatrix A = new Array2DRowRealMatrix(3 * n, 7);
        RealVector L = new ArrayRealVector(3 * n);

        for (int i = 0; i < n; i++) {
            // Центрированные координаты исходной системы
            double x = from[i][0] - fromCentroid[0];
            double y = from[i][1] - fromCentroid[1];
            double z = from[i][2] - fromCentroid[2];

            // Центрированные координаты целевой системы
            double xp = to[i][0] - toCentroid[0];
            double yp = to[i][1] - toCentroid[1];
            double zp = to[i][2] - toCentroid[2];

            int row = i * 3;

            // Матрица A: столбцы [dx, dy, dz, rx, ry, rz, ds]
            // ∂X/∂dx = 1, ∂X/∂dy = 0, ∂X/∂dz = 0
            A.setEntry(row, 0, 1);
            A.setEntry(row + 1, 1, 1);
            A.setEntry(row + 2, 2, 1);

            // ∂X/∂rx = 0, ∂X/∂ry = z, ∂X/∂rz = -y
            A.setEntry(row, 3, 0);
            A.setEntry(row, 4, z);
            A.setEntry(row, 5, -y);

            // ∂Y/∂rx = -z, ∂Y/∂ry = 0, ∂Y/∂rz = x
            A.setEntry(row + 1, 3, -z);
            A.setEntry(row + 1, 4, 0);
            A.setEntry(row + 1, 5, x);

            // ∂Z/∂rx = y, ∂Z/∂ry = -x, ∂Z/∂rz = 0
            A.setEntry(row + 2, 3, y);
            A.setEntry(row + 2, 4, -x);
            A.setEntry(row + 2, 5, 0);

            // ∂X/∂ds = x, ∂Y/∂ds = y, ∂Z/∂ds = z
            A.setEntry(row, 6, x);
            A.setEntry(row + 1, 6, y);
            A.setEntry(row + 2, 6, z);

            // ✅ ИСПРАВЛЕНО: свободные члены = (to − C_to) − (from − C_from)
            L.setEntry(row,     xp - x);
            L.setEntry(row + 1, yp - y);
            L.setEntry(row + 2, zp - z);
        }

        RealVector delta;
        try {
            delta = new QRDecomposition(A).getSolver().solve(L);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Не удалось решить систему для HELMERT_7: " + e.getMessage(), e);
        }

        // Восстанавливаем абсолютные параметры
        double[] params = new double[7];
        params[0] = delta.getEntry(0) + toCentroid[0] - fromCentroid[0];
        params[1] = delta.getEntry(1) + toCentroid[1] - fromCentroid[1];
        params[2] = delta.getEntry(2) + toCentroid[2] - fromCentroid[2];
        params[3] = delta.getEntry(3) * 206264.806;  // радианы → угловые секунды
        params[4] = delta.getEntry(4) * 206264.806;
        params[5] = delta.getEntry(5) * 206264.806;
        params[6] = delta.getEntry(6) * 1e6;         // безразм. → ppm

        double rms = computeRms(from, to, p -> applyHelmert(p, params));

        log.info("HELMERT_7: dx = {}, dy = {}, dz = {}, "
                        + "rx = {}″, ry = {}″, rz = {}″, ds = {} ppm, RMS = {}",
                params[0], params[1], params[2],
                params[3], params[4], params[5], params[6], rms);

        return new Result(Model.HELMERT_7, params, rms, 1);
    }

    // ================================================================
    // 12-параметрическое (AFFINE_12)
    // ================================================================

    /**
     * Оценка 12 параметров аффинного преобразования: матрица 3×4,
     * применяемая к вектору [x, y, z, 1].
     * <p>
     * Модель: to = A · [from; 1]. Решается МНК отдельно для каждой
     * выходной координаты.
     */
    private Result estimateAffine(double[][] from, double[][] to, int n) {
        for (int i = 0; i < n; i++) {
            validatePoint(from[i], 3, "from[" + i + "]");
            validatePoint(to[i], 3, "to[" + i + "]");
        }

        // Матрица A: [n × 4] — [x, y, z, 1]
        RealMatrix A = new Array2DRowRealMatrix(n, 4);
        for (int i = 0; i < n; i++) {
            A.setEntry(i, 0, from[i][0]);
            A.setEntry(i, 1, from[i][1]);
            A.setEntry(i, 2, from[i][2]);
            A.setEntry(i, 3, 1.0);
        }

        // Нормальная матрица для МНК: (AᵀA)⁻¹ Aᵀ
        RealMatrix AtA = A.transpose().multiply(A);
        RealMatrix AtAinv;
        try {
            AtAinv = new LUDecomposition(AtA).getSolver().getInverse();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Матрица AᵀA вырождена — проверьте, что точки не коллинеарны", e);
        }
        RealMatrix At = A.transpose();
        RealMatrix AtAinvAt = AtAinv.multiply(At);

        // Решаем три отдельные задачи для каждой координаты
        double[] params = new double[12];
        for (int coord = 0; coord < 3; coord++) {
            RealVector b = new ArrayRealVector(n);
            for (int i = 0; i < n; i++) {
                b.setEntry(i, to[i][coord]);
            }
            RealVector x = AtAinvAt.operate(b);
            for (int j = 0; j < 4; j++) {
                params[coord * 4 + j] = x.getEntry(j);
            }
        }

        double rms = computeRms(from, to, p -> applyAffine(p, params));

        log.info("AFFINE_12: RMS = {} (n = {})", rms, n);

        return new Result(Model.AFFINE_12, params, rms, 1);
    }

    // ================================================================
    // Вспомогательные методы
    // ================================================================

    /** Проверка базовых входных массивов. */
    private void validateArrays(double[][] from, double[][] to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Массивы точек не могут быть null");
        }
        if (from.length != to.length) {
            throw new IllegalArgumentException(
                    "Разное число точек: from = " + from.length
                            + ", to = " + to.length);
        }
        if (from.length < MIN_POINTS) {
            throw new IllegalArgumentException(
                    "Нужно минимум " + MIN_POINTS + " общие точки, получено "
                            + from.length);
        }
    }

    /** Проверяет наличие третьей координаты во всех точках. */
    private boolean hasThirdCoordinate(double[][] points) {
        if (points == null || points.length == 0) return false;
        for (double[] p : points) {
            if (p == null || p.length < 3) return false;
        }
        return true;
    }

    /** Проверяет корректность одной точки. */
    private void validatePoint(double[] p, int dims, String label) {
        if (p == null) {
            throw new IllegalArgumentException(
                    "Точка " + label + " не может быть null");
        }
        if (p.length < dims) {
            throw new IllegalArgumentException(
                    "Точка " + label + " имеет размерность " + p.length
                            + ", ожидалось минимум " + dims);
        }
        for (int d = 0; d < dims; d++) {
            if (!Double.isFinite(p[d])) {
                throw new IllegalArgumentException(
                        "Точка " + label + " содержит некорректное значение "
                                + "координаты " + d + ": " + p[d]);
            }
        }
    }

    /** Центроид точек. */
    private double[] centroid(double[][] points, int n) {
        double[] c = new double[3];
        for (int i = 0; i < n; i++) {
            c[0] += points[i][0];
            c[1] += points[i][1];
            c[2] += points[i][2];
        }
        c[0] /= n;
        c[1] /= n;
        c[2] /= n;
        return c;
    }

    /** Применение HELMERT_7 к одной точке. */
    private double[] applyHelmert(double[] p, double[] params) {
        double dx = params[0];
        double dy = params[1];
        double dz = params[2];
        double rx = params[3] / 206264.806;
        double ry = params[4] / 206264.806;
        double rz = params[5] / 206264.806;
        double ds = params[6] * 1e-6;

        // Матрица поворота для малых углов
        double[][] R = {
                {1,  -rz, ry},
                {rz, 1,  -rx},
                {-ry, rx, 1}
        };
        double scale = 1 + ds;

        return new double[]{
                dx + scale * (R[0][0] * p[0] + R[0][1] * p[1] + R[0][2] * p[2]),
                dy + scale * (R[1][0] * p[0] + R[1][1] * p[1] + R[1][2] * p[2]),
                dz + scale * (R[2][0] * p[0] + R[2][1] * p[1] + R[2][2] * p[2])
        };
    }

    /** Применение AFFINE_12 к одной точке. */
    private double[] applyAffine(double[] p, double[] params) {
        return new double[]{
                params[0] * p[0] + params[1] * p[1] + params[2] * p[2] + params[3],
                params[4] * p[0] + params[5] * p[1] + params[6] * p[2] + params[7],
                params[8] * p[0] + params[9] * p[1] + params[10] * p[2] + params[11]
        };
    }

    /**
     * RMS-ошибка трансформации. Корректно работает для 2D и 3D точек:
     * третья координата учитывается только если присутствует в обеих
     * точках.
     */
    private double computeRms(double[][] from, double[][] to,
                              java.util.function.Function<double[], double[]> transform) {
        double sumSq = 0;
        int count = 0;

        for (int i = 0; i < from.length; i++) {
            if (from[i] == null || to[i] == null) continue;

            double[] transformed = transform.apply(from[i]);

            double dx = transformed[0] - to[i][0];
            double dy = transformed[1] - to[i][1];
            double dz = 0.0;

            if (transformed.length > 2 && to[i].length > 2) {
                dz = transformed[2] - to[i][2];
            }

            sumSq += dx * dx + dy * dy + dz * dz;
            count++;
        }

        if (count == 0) {
            throw new IllegalArgumentException(
                    "Нет валидных точек для вычисления RMS");
        }
        return Math.sqrt(sumSq / count);
    }
}