package ru.geodesy.adjust.transformation.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.transformation.api.CoordinateSystem;
import ru.geodesy.adjust.transformation.api.CoordinateTransformer;
import ru.geodesy.adjust.transformation.api.TransformationMethod;
import ru.geodesy.common.ValidationException;

/**
 * 7-параметрическое преобразование Бурса-Вольфа.
 * Использует полную матрицу поворота (не малоугловое приближение).
 */
@Slf4j
@Component
public class HelmertTransformer implements CoordinateTransformer {

    @Override
    public TransformationMethod method() {
        return TransformationMethod.HELMERT;
    }

    @Override
    public double[] transform(double[] point, CoordinateSystem from, CoordinateSystem to) {
        if (from.toWgs84() == null || to.toWgs84() == null) {
            throw new ValidationException(
                    "Обе системы должны содержать параметры toWgs84");
        }

        validateLatitude(point[0], "lat");
        validateLongitude(point[1], "lon");

        double[] geocentric = toGeocentric(point, from.ellipsoid());
        double[] toWgs = applyBursaWolf(geocentric, from.toWgs84());
        double[] fromWgs = applyInverseBursaWolf(toWgs, to.toWgs84());
        return fromGeocentric(fromWgs, to.ellipsoid());
    }

    // ============================================================
    // Бурса-Вольфа
    // ============================================================

    private double[] applyBursaWolf(double[] xyz, double[] params) {
        double dx = params[0], dy = params[1], dz = params[2];
        double rx = Math.toRadians(params[3] / 3600.0);
        double ry = Math.toRadians(params[4] / 3600.0);
        double rz = Math.toRadians(params[5] / 3600.0);
        double ds = params[6] * 1e-6;

        double[][] R = buildRotationMatrix(rx, ry, rz);
        double scale = 1 + ds;

        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            result[i] = dx + scale * (R[i][0] * xyz[0]
                    + R[i][1] * xyz[1]
                    + R[i][2] * xyz[2]);
        }
        return result;
    }

    private double[] applyInverseBursaWolf(double[] xyz, double[] params) {
        double dx = params[0], dy = params[1], dz = params[2];
        double rx = Math.toRadians(params[3] / 3600.0);
        double ry = Math.toRadians(params[4] / 3600.0);
        double rz = Math.toRadians(params[5] / 3600.0);
        double ds = params[6] * 1e-6;

        double[][] R = buildRotationMatrix(rx, ry, rz);
        double scale = 1 + ds;
        double invScale = 1.0 / scale;

        double[] shifted = {
                xyz[0] - dx,
                xyz[1] - dy,
                xyz[2] - dz
        };

        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            result[i] = invScale * (R[0][i] * shifted[0]
                    + R[1][i] * shifted[1]
                    + R[2][i] * shifted[2]);
        }
        return result;
    }

    private double[][] buildRotationMatrix(double rx, double ry, double rz) {
        double cx = Math.cos(rx), sx = Math.sin(rx);
        double cy = Math.cos(ry), sy = Math.sin(ry);
        double cz = Math.cos(rz), sz = Math.sin(rz);

        return new double[][]{
                {cy * cz, cy * sz, -sy},
                {sx * sy * cz - cx * sz, sx * sy * sz + cx * cz, sx * cy},
                {cx * sy * cz + sx * sz, cx * sy * sz - sx * cz, cx * cy}
        };
    }

    // ============================================================
    // Геодезические ↔ геоцентрические
    // ============================================================

    public double[] toGeocentric(double[] point, Ellipsoid ellipsoid) {
        double lat = point[0];
        double lon = point[1];
        double h = point.length > 2 ? point[2] : 0.0;

        double a = ellipsoid.semiMajorAxis();
        double e2 = ellipsoid.eccentricitySquared();
        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double N = a / Math.sqrt(1 - e2 * sinLat * sinLat);

        double X = (N + h) * cosLat * Math.cos(lon);
        double Y = (N + h) * cosLat * Math.sin(lon);
        double Z = (N * (1 - e2) + h) * sinLat;
        return new double[]{X, Y, Z};
    }

    public double[] fromGeocentric(double[] xyz, Ellipsoid ellipsoid) {
        double X = xyz[0], Y = xyz[1], Z = xyz[2];
        double a = ellipsoid.semiMajorAxis();
        double e2 = ellipsoid.eccentricitySquared();

        double p = Math.sqrt(X * X + Y * Y);
        double lon = Math.atan2(Y, X);

        if (p < 1e-9) {
            double lat = Z >= 0 ? Math.PI / 2 : -Math.PI / 2;
            double h = Math.abs(Z) - ellipsoid.semiMinorAxis();
            return new double[]{lat, lon, h};
        }

        double lat = Math.atan2(Z, p * (1 - e2));
        double latPrev;
        double N;
        double h;
        int iter = 0;
        do {
            latPrev = lat;
            double sinLat = Math.sin(lat);
            N = a / Math.sqrt(1 - e2 * sinLat * sinLat);
            h = p / Math.cos(lat) - N;
            lat = Math.atan2(Z, p * (1 - e2 * N / (N + h)));
            iter++;
        } while (Math.abs(lat - latPrev) > 1e-12 && iter < 20);

        double sinLat = Math.sin(lat);
        N = a / Math.sqrt(1 - e2 * sinLat * sinLat);
        h = p / Math.cos(lat) - N;

        return new double[]{lat, lon, h};
    }

    private void validateLatitude(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new ValidationException(
                    "Некорректная широта (" + label + "): " + value);
        }
        if (Math.abs(value) > Math.PI / 2 + 1e-9) {
            throw new ValidationException(
                    "Широта вне диапазона [-π/2, π/2]: " + value);
        }
    }

    private void validateLongitude(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new ValidationException(
                    "Некорректная долгота (" + label + "): " + value);
        }
        if (Math.abs(value) > Math.PI + 1e-9) {
            throw new ValidationException(
                    "Долгота вне диапазона [-π, π]: " + value);
        }
    }
}