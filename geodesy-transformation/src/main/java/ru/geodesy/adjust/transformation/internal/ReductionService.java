package ru.geodesy.adjust.transformation.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.DistanceMeasurement;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.common.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Редуцирование измерений на поверхность относимости.
 * <p>
 * Поскольку {@link Measurement} неизменяем, метод {@link #reduceNetwork}
 * возвращает <b>новую</b> сеть с исправленными наблюдениями.
 */
@Slf4j
@Component
public class ReductionService {

    private static final double MEAN_EARTH_RADIUS = Constants.EARTH_RADIUS_MEAN;
    private static final double DEFAULT_REFRACTION = 0.13;

    /**
     * Возвращает НОВУЮ сеть с редуцированными измерениями.
     */
    public Network reduceNetwork(Network network) {
        Map<String, Point> pointMap = new HashMap<>();
        for (Point p : network.points()) {
            pointMap.put(p.id(), p);
        }

        ParameterSet params = ParameterSet.fromPoints(network.points());

        List<Measurement> reduced = new ArrayList<>(network.measurements().size());
        int reducedCount = 0;

        for (Measurement m : network.measurements()) {
            if (m instanceof DistanceMeasurement dm) {
                DistanceMeasurement newDm = reduceDistance(dm, pointMap, params);
                reduced.add(newDm);
                if (Math.abs(newDm.getObserved() - dm.getObserved()) > 1e-9) {
                    reducedCount++;
                }
            } else {
                reduced.add(m);
            }
        }

        log.info("Редукция: {} из {} измерений изменены",
                reducedCount, network.measurements().size());

        return network.withMeasurements(reduced);
    }

    private DistanceMeasurement reduceDistance(DistanceMeasurement dm,
                                               Map<String, Point> pointMap,
                                               ParameterSet params) {
        Point from = pointMap.get(dm.fromPoint());
        Point to = pointMap.get(dm.toPoint());
        if (from == null || to == null) return dm;

        double S = dm.getObserved();
        double reduced = S;

        if (from.z() != null && to.z() != null) {
            double h = to.z() - from.z();
            reduced = slopeToHorizontal(reduced, h);
        }

        if (from.y() != null && to.y() != null) {
            reduced += gaussKrugerReduction(reduced, from.y(), to.y());
        }

        if (Math.abs(reduced - S) < 1e-9) return dm;

        return new DistanceMeasurement(
                reduced, dm.getVariance(),
                dm.fromPoint(), dm.toPoint(),
                params,
                dm.accuracyClass(), dm.instrument(), dm.tag());
    }

    public double slopeToHorizontal(double S_slope, double heightDiff) {
        if (S_slope <= 0) return 0;
        return S_slope - (heightDiff * heightDiff) / (2.0 * S_slope);
    }

    public double gaussKrugerReduction(double S, double y1, double y2) {
        double ym = (y1 + y2) / 2.0;
        double dy = y2 - y1;
        return S * (ym * ym) / (2.0 * MEAN_EARTH_RADIUS * MEAN_EARTH_RADIUS)
                + (dy * dy) / (24.0 * MEAN_EARTH_RADIUS * MEAN_EARTH_RADIUS);
    }

    public double curvatureRefractionCorrection(double S, double refractionCoeff) {
        return S * S * (1.0 - refractionCoeff) / (2.0 * MEAN_EARTH_RADIUS);
    }

    public double curvatureRefractionCorrection(double S) {
        return curvatureRefractionCorrection(S, DEFAULT_REFRACTION);
    }

    public double curvatureRadius(double lat, Ellipsoid ellipsoid) {
        double a = ellipsoid.semiMajorAxis();
        double e2 = ellipsoid.eccentricitySquared();
        double sinLat = Math.sin(lat);
        return a * (1 - e2) / Math.pow(1 - e2 * sinLat * sinLat, 1.5);
    }
}