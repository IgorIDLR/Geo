package ru.geodesy.adjust.design.api;

import java.util.Map;

/**
 * Результат анализа ожидаемой точности.
 */
public record PrecisionResult(
        Map<String, double[]> stdDevCoordinates,
        Map<String, double[]> errorEllipses,
        double maxStdDev,
        String worstPoint,
        long elapsedMillis
) {

    public double getStdX(String pointId) {
        double[] std = stdDevCoordinates.get(pointId);
        return std != null ? std[0] : Double.NaN;
    }

    public double getStdY(String pointId) {
        double[] std = stdDevCoordinates.get(pointId);
        return std != null ? std[1] : Double.NaN;
    }

    public double getStdZ(String pointId) {
        double[] std = stdDevCoordinates.get(pointId);
        return std != null ? std[2] : Double.NaN;
    }

    public double getSemiMajorAxis(String pointId) {
        double[] ellipse = errorEllipses.get(pointId);
        return ellipse != null ? ellipse[0] : Double.NaN;
    }

    public double getSemiMinorAxis(String pointId) {
        double[] ellipse = errorEllipses.get(pointId);
        return ellipse != null ? ellipse[1] : Double.NaN;
    }

    public double getEllipseOrientation(String pointId) {
        double[] ellipse = errorEllipses.get(pointId);
        return ellipse != null ? ellipse[2] : Double.NaN;
    }
}