package ru.geodesy.adjust.transformation.internal;

import org.springframework.stereotype.Component;
import ru.geodesy.adjust.transformation.api.CoordinateSystem;
import ru.geodesy.adjust.transformation.api.CoordinateTransformer;
import ru.geodesy.adjust.transformation.api.TransformationMethod;
import ru.geodesy.common.Preconditions;
import ru.geodesy.common.ValidationException;

/**
 * Проекция Гаусса-Крюгера по формулам Крагера до 6-го порядка.
 */
@Component
public class GaussKrugerTransformer implements CoordinateTransformer {

    private static final double POLE_EPSILON = 1e-9;

    @Override
    public TransformationMethod method() {
        return TransformationMethod.GAUSS_KRUGER;
    }

    @Override
    public double[] transform(double[] point, CoordinateSystem from, CoordinateSystem to) {
        boolean fromGeographic = from.isGeographic();
        boolean toGeographic = to.isGeographic();

        if (fromGeographic && !toGeographic) {
            return projectToPlane(point, to);
        }
        if (!fromGeographic && toGeographic) {
            return projectToGeographic(point, from);
        }
        double[] geo = projectToGeographic(point, from);
        return projectToPlane(geo, to);
    }

    // ============================================================
    // Прямая задача
    // ============================================================

    public double[] projectToPlane(double[] geographic, CoordinateSystem target) {
        Preconditions.checkState(target.hasProjection(),
                "Целевая система должна содержать проекцию");

        double lat = geographic[0];
        double lon = geographic[1];

        if (Math.abs(Math.cos(lat)) < POLE_EPSILON) {
            throw new ValidationException(
                    "Проекция Гаусса-Крюгера не определена на полюсе");
        }

        var proj = target.projection();
        double lon0 = Math.toRadians(proj.centralMeridian());
        double k0 = proj.scaleFactor() > 0 ? proj.scaleFactor() : 1.0;
        double falseE = proj.falseEasting();
        double falseN = proj.falseNorthing();

        Ellipsoid e = target.ellipsoid();
        double a = e.semiMajorAxis();
        double e2 = e.eccentricitySquared();
        double ep2 = e.secondEccentricitySquared();

        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double tanLat = Math.tan(lat);

        double N = a / Math.sqrt(1 - e2 * sinLat * sinLat);
        double T = tanLat * tanLat;
        double C = ep2 * cosLat * cosLat;
        double A = (lon - lon0) * cosLat;

        double M = meridionalArc(lat, a, e2);

        double A2 = A * A, A3 = A2 * A, A4 = A3 * A;
        double A5 = A4 * A, A6 = A5 * A;

        double x = k0 * (M + N * tanLat * (A2 / 2
                + (5 - T + 9 * C + 4 * C * C) * A4 / 24
                + (61 - 58 * T + T * T + 600 * C - 330 * ep2) * A6 / 720))
                + falseN;

        double y = k0 * N * (A
                + (1 - T + C) * A3 / 6
                + (5 - 18 * T + T * T + 72 * C - 58 * ep2) * A5 / 120)
                + falseE;

        return new double[]{x, y, geographic.length > 2 ? geographic[2] : 0.0};
    }

    // ============================================================
    // Обратная задача
    // ============================================================

    public double[] projectToGeographic(double[] planar, CoordinateSystem source) {
        Preconditions.checkState(source.hasProjection(),
                "Исходная система должна содержать проекцию");

        double x = planar[0];
        double y = planar[1];

        var proj = source.projection();
        double lon0 = Math.toRadians(proj.centralMeridian());
        double k0 = proj.scaleFactor() > 0 ? proj.scaleFactor() : 1.0;
        double falseE = proj.falseEasting();
        double falseN = proj.falseNorthing();

        Ellipsoid e = source.ellipsoid();
        double a = e.semiMajorAxis();
        double e2 = e.eccentricitySquared();
        double ep2 = e.secondEccentricitySquared();

        double M = (x - falseN) / k0;
        double mu = M / (a * (1 - e2 / 4 - 3 * e2 * e2 / 64
                - 5 * e2 * e2 * e2 / 256));

        double e1 = (1 - Math.sqrt(1 - e2)) / (1 + Math.sqrt(1 - e2));

        double phi1 = mu
                + (3 * e1 / 2 - 27 * e1 * e1 * e1 / 32) * Math.sin(2 * mu)
                + (21 * e1 * e1 / 16 - 55 * e1 * e1 * e1 * e1 / 32) * Math.sin(4 * mu)
                + (151 * e1 * e1 * e1 / 96) * Math.sin(6 * mu);

        double sinPhi1 = Math.sin(phi1);
        double cosPhi1 = Math.cos(phi1);
        double tanPhi1 = Math.tan(phi1);

        double N1 = a / Math.sqrt(1 - e2 * sinPhi1 * sinPhi1);
        double T1 = tanPhi1 * tanPhi1;
        double C1 = ep2 * cosPhi1 * cosPhi1;
        double R1 = a * (1 - e2) / Math.pow(1 - e2 * sinPhi1 * sinPhi1, 1.5);
        double D = (y - falseE) / (N1 * k0);

        double D2 = D * D, D3 = D2 * D, D4 = D3 * D;
        double D5 = D4 * D, D6 = D5 * D;

        double lat = phi1 - (N1 * tanPhi1 / R1) * (D2 / 2
                - (5 + 3 * T1 + 10 * C1 - 4 * C1 * C1 - 9 * ep2) * D4 / 24
                + (61 + 90 * T1 + 298 * C1 + 45 * T1 * T1
                - 252 * ep2 - 3 * C1 * C1) * D6 / 720);

        double lon = lon0 + (D
                - (1 + 2 * T1 + C1) * D3 / 6
                + (5 - 2 * C1 + 28 * T1 - 3 * C1 * C1
                + 8 * ep2 + 24 * T1 * T1) * D5 / 120) / cosPhi1;

        return new double[]{lat, lon, planar.length > 2 ? planar[2] : 0.0};
    }

    private double meridionalArc(double lat, double a, double e2) {
        double e4 = e2 * e2, e6 = e4 * e2;
        return a * ((1 - e2 / 4 - 3 * e4 / 64 - 5 * e6 / 256) * lat
                - (3 * e2 / 8 + 3 * e4 / 32 + 45 * e6 / 1024) * Math.sin(2 * lat)
                + (15 * e4 / 256 + 45 * e6 / 1024) * Math.sin(4 * lat)
                - (35 * e6 / 3072) * Math.sin(6 * lat));
    }
}