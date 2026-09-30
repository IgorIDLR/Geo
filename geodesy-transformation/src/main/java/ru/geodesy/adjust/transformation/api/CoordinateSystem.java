package ru.geodesy.adjust.transformation.api;

import ru.geodesy.adjust.transformation.internal.Ellipsoid;
import ru.geodesy.common.Preconditions;

/**
 * Описание системы координат: ID, эллипсоид, проекция и параметры
 * перехода к WGS84 (7 параметров Гельмерта).
 */
public record CoordinateSystem(
        String id,
        String name,
        Ellipsoid ellipsoid,
        Projection projection,
        double[] toWgs84
) {

    public record Projection(
            ProjectionType type,
            double centralMeridian,
            double falseEasting,
            double falseNorthing,
            double scaleFactor
    ) {
        public enum ProjectionType {
            NONE,
            GAUSS_KRUGER,
            UTM,
            LCC,
            PROJ4J
        }
    }

    public CoordinateSystem {
        Preconditions.requireNonBlank(id, "coordinateSystem.id");
        Preconditions.requireNonNull(ellipsoid, "coordinateSystem.ellipsoid");
        if (toWgs84 != null && toWgs84.length != 7) {
            throw new IllegalArgumentException(
                    "toWgs84 должен содержать 7 элементов");
        }
    }

    public boolean hasProjection() {
        return projection != null
                && projection.type() != Projection.ProjectionType.NONE;
    }

    public boolean hasToWgs84() {
        return toWgs84 != null;
    }

    public boolean isGeographic() {
        return !hasProjection();
    }
}