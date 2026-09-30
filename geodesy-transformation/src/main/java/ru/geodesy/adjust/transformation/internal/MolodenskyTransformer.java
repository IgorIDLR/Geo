package ru.geodesy.adjust.transformation.internal;

import org.springframework.stereotype.Component;
import ru.geodesy.adjust.transformation.api.CoordinateSystem;
import ru.geodesy.adjust.transformation.api.CoordinateTransformer;
import ru.geodesy.adjust.transformation.api.TransformationMethod;
import ru.geodesy.common.Preconditions;

/**
 * 3-параметрическое преобразование Молоденского.
 * Работает в геодезических координатах (B, L, H).
 */
@Component
public class MolodenskyTransformer implements CoordinateTransformer {

    @Override
    public TransformationMethod method() {
        return TransformationMethod.MOLODENSKY;
    }

    @Override
    public double[] transform(double[] point, CoordinateSystem from, CoordinateSystem to) {
        Preconditions.checkState(from.hasToWgs84() && to.hasToWgs84(),
                "Обе системы должны содержать параметры toWgs84");

        double[] fromParams = from.toWgs84();
        double[] toParams = to.toWgs84();

        double dx = toParams[0] - fromParams[0];
        double dy = toParams[1] - fromParams[1];
        double dz = toParams[2] - fromParams[2];

        double lat = point[0];
        double lon = point[1];
        double h = point.length > 2 ? point[2] : 0.0;

        double aFrom = from.ellipsoid().semiMajorAxis();
        double fFrom = from.ellipsoid().flattening();
        double aTo = to.ellipsoid().semiMajorAxis();
        double fTo = to.ellipsoid().flattening();

        double da = aTo - aFrom;
        double df = fTo - fFrom;

        double e2 = from.ellipsoid().eccentricitySquared();
        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double sin2Lat = sinLat * sinLat;
        double W = Math.sqrt(1 - e2 * sin2Lat);
        double M = aFrom * (1 - e2) / (W * W * W);
        double N = aFrom / W;

        double dLat = (-dx * sinLat * cosLat - dy * sinLat * sinLat
                + dz * cosLat
                + da * (N * e2 * sinLat * cosLat) / aFrom
                + df * (M / (1 - fFrom) + N * (1 - fFrom)) * sinLat * cosLat)
                / (M + h);

        double dLon = (-dx * sinLat + dy * cosLat) / ((N + h) * cosLat);

        double dH = dx * cosLat * cosLat + dy * cosLat * sinLat
                + dz * sinLat
                - da * aFrom / N
                + df * (1 - fFrom) * aFrom * sin2Lat / W;

        return new double[]{lat + dLat, lon + dLon, h + dH};
    }
}