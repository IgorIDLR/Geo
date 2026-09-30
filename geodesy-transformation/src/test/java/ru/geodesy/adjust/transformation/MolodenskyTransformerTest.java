package ru.geodesy.adjust.transformation;

import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.transformation.api.CoordinateSystem;
import ru.geodesy.adjust.transformation.internal.Ellipsoid;
import ru.geodesy.adjust.transformation.internal.MolodenskyTransformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MolodenskyTransformerTest {

    private final MolodenskyTransformer transformer = new MolodenskyTransformer();

    @Test
    void transformSk42ToWgs84ProducesShift() {
        CoordinateSystem sk42 = new CoordinateSystem(
                "SK42", "СК-42", Ellipsoid.KRASSOVSKY, null,
                new double[]{23.92, -141.27, -80.9, 0, 0, 0, 0});
        CoordinateSystem wgs84 = new CoordinateSystem(
                "WGS84", "WGS84", Ellipsoid.WGS84, null,
                new double[]{0, 0, 0, 0, 0, 0, 0});

        double[] point = {
                Math.toRadians(55.7558),
                Math.toRadians(37.6173),
                150.0
        };

        double[] result = transformer.transform(point, sk42, wgs84);

        // Широта и долгота должны сдвинуться
        assertThat(result[0]).isNotEqualTo(point[0]);
        assertThat(result[1]).isNotEqualTo(point[1]);

        // Сдвиг в пределах десятков секунд
        double dLat = Math.abs(result[0] - point[0]) * 206264.806;
        double dLon = Math.abs(result[1] - point[1]) * 206264.806;
        assertThat(dLat).isLessThan(60);
        assertThat(dLon).isLessThan(60);
    }

    @Test
    void sameSystemProducesZeroShift() {
        CoordinateSystem sys = new CoordinateSystem(
                "TEST", "Test", Ellipsoid.WGS84, null,
                new double[]{0, 0, 0, 0, 0, 0, 0});

        double[] point = {
                Math.toRadians(55.0),
                Math.toRadians(37.0),
                100.0
        };

        double[] result = transformer.transform(point, sys, sys);

        assertThat(result[0]).isCloseTo(point[0], within(1e-12));
        assertThat(result[1]).isCloseTo(point[1], within(1e-12));
        assertThat(result[2]).isCloseTo(point[2], within(1e-6));
    }
}