package ru.geodesy.adjust.deformation.api;

/**
 * Смещение одной точки между эпохами.
 */
public record PointDisplacement(
        String pointId,
        double dx, double dy, double dz,
        double sigmaDx, double sigmaDy, double sigmaDz,
        double magnitude,
        boolean significant,
        double confidenceLevel
) {

    public double getMagnitude2D() {
        return Math.hypot(dx, dy);
    }

    public double getHorizontalSignificance() {
        return sigmaDx > 0 && sigmaDy > 0
                ? getMagnitude2D() / Math.hypot(sigmaDx, sigmaDy)
                : 0;
    }
}