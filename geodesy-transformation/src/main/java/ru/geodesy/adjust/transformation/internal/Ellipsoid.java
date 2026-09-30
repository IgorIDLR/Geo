package ru.geodesy.adjust.transformation.internal;

/**
 * Параметры эллипсоида: большая полуось и сжатие.
 */
public record Ellipsoid(
        String name,
        double semiMajorAxis,
        double flattening
) {

    public static final Ellipsoid WGS84 = new Ellipsoid(
            "WGS84", 6378137.0, 1.0 / 298.257223563);

    public static final Ellipsoid KRASSOVSKY = new Ellipsoid(
            "Красовский", 6378245.0, 1.0 / 298.3);

    public static final Ellipsoid GRS80 = new Ellipsoid(
            "GRS80", 6378137.0, 1.0 / 298.257222101);

    public double semiMinorAxis() {
        return semiMajorAxis * (1 - flattening);
    }

    public double eccentricitySquared() {
        return flattening * (2 - flattening);
    }

    public double secondEccentricitySquared() {
        double e2 = eccentricitySquared();
        return e2 / (1 - e2);
    }

    public double meanRadius() {
        return semiMajorAxis * (1 - flattening / 3);
    }
}