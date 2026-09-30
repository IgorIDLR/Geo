package ru.geodesy.adjust.transformation.internal;

import org.locationtech.proj4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.transformation.api.CoordinateSystem;
import ru.geodesy.adjust.transformation.api.CoordinateTransformer;
import ru.geodesy.adjust.transformation.api.TransformationMethod;
import ru.geodesy.common.ValidationException;

/**
 * Универсальный трансформер через Proj4J.
 * Активируется только при наличии Proj4J в classpath (профиль full).
 */
@Component
@ConditionalOnClass(name = "org.locationtech.proj4j.CRSFactory")
public class Proj4JTransformer implements CoordinateTransformer {

    private final CRSFactory crsFactory = new CRSFactory();

    @Override
    public TransformationMethod method() {
        return TransformationMethod.PROJ4J;
    }

    @Override
    public double[] transform(double[] point, CoordinateSystem from, CoordinateSystem to) {
        try {
            var src = crsFactory.createFromName(from.id());
            var dst = crsFactory.createFromName(to.id());
            var transform = new BasicCoordinateTransform(src, dst);

            ProjCoordinate in = new ProjCoordinate(point[0], point[1]);
            ProjCoordinate out = new ProjCoordinate();
            transform.transform(in, out);

            return new double[]{out.x, out.y, point.length > 2 ? point[2] : 0.0};
        } catch (Exception e) {
            throw new ValidationException(
                    "Proj4J трансформация не удалась: " + e.getMessage(), e);
        }
    }
}