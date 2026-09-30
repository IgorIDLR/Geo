package ru.geodesy.adjust.transformation.api;

/**
 * Публичный интерфейс трансформации координат.
 * <p>
 * Формат точки:
 * <ul>
 *   <li>Географическая система: [lat, lon, h] (радианы, метры)</li>
 *   <li>Плоская система: [x, y, h] (метры)</li>
 * </ul>
 */
public interface CoordinateTransformer {

    double[] transform(double[] point,
                       CoordinateSystem from,
                       CoordinateSystem to);

    default double[][] transformAll(double[][] points,
                                    CoordinateSystem from,
                                    CoordinateSystem to) {
        double[][] result = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            result[i] = transform(points[i], from, to);
        }
        return result;
    }

    TransformationMethod method();
}