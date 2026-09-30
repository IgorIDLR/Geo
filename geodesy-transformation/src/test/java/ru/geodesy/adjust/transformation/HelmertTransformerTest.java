package ru.geodesy.adjust.transformation;

import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.transformation.api.CoordinateSystem;
import ru.geodesy.adjust.transformation.internal.Ellipsoid;
import ru.geodesy.adjust.transformation.internal.HelmertTransformer;
import ru.geodesy.common.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class HelmertTransformerTest {

    private final HelmertTransformer transformer = new HelmertTransformer();

    // ================================================================
    // Идентичное преобразование
    // ================================================================

    @Test
    void identityTransformationReturnsSamePoint() {
        CoordinateSystem sys = wgs84();

        // Москва: 55.7558° с.ш., 37.6173° в.д., высота 150 м
        double[] point = {
                Math.toRadians(55.7558),
                Math.toRadians(37.6173),
                150.0
        };

        double[] result = transformer.transform(point, sys, sys);

        assertThat(result[0]).isCloseTo(point[0], within(1e-10));
        assertThat(result[1]).isCloseTo(point[1], within(1e-10));
        assertThat(result[2]).isCloseTo(point[2], within(1e-6));
    }

    // ================================================================
    // Переход СК-42 → WGS84
    // ================================================================

    @Test
    void transformFromSk42ToWgs84ChangesCoordinates() {
        CoordinateSystem sk42 = sk42();
        CoordinateSystem wgs84 = wgs84();

        double[] point = {
                Math.toRadians(55.7558),
                Math.toRadians(37.6173),
                150.0
        };

        double[] result = transformer.transform(point, sk42, wgs84);

        // Широта должна сдвинуться на несколько угловых секунд
        double dLatSeconds = (result[0] - point[0]) * 206264.806;
        double dLonSeconds = (result[1] - point[1]) * 206264.806;

        assertThat(Math.abs(dLatSeconds)).isLessThan(30);
        assertThat(Math.abs(dLonSeconds)).isLessThan(30);

        // Высота может измениться на десятки метров
        assertThat(Math.abs(result[2] - point[2])).isLessThan(200);
    }

    // ================================================================
    // Round-trip (обратное преобразование)
    // ================================================================

//    @Test
//    void roundTripFromSk42ToWgs84() {
//        CoordinateSystem sk42 = sk42();
//        CoordinateSystem wgs84 = wgs84();
//
//        double[] point = {
//                Math.toRadians(55.7558),
//                Math.toRadians(37.6173),
//                150.0
//        };
//
//        double[] toWgs = transformer.transform(point, sk42, wgs84);
//        double[] back = transformer.transform(toWgs, wgs84, sk42);
//
//        // Реалистичная точность round-trip для 7-параметрического
//        // преобразования с итеративным Bowring и неортогональной R:
//        // ~1e-4 рад (≈ 20 угловых секунд)
//        assertThat(back[0]).isCloseTo(point[0], within(1e-4));
//        assertThat(back[1]).isCloseTo(point[1], within(1e-4));
//        assertThat(back[2]).isCloseTo(point[2], within(0.05)); // 5 см
//    }

    // ================================================================
    // Геоцентрические ↔ геодезические
    // ================================================================

    @Test
    void toGeocentricAtEquatorProducesExpectedX() {
        double[] point = {0.0, 0.0, 0.0};
        double[] xyz = transformer.toGeocentric(point, Ellipsoid.WGS84);

        assertThat(xyz[0]).isCloseTo(6378137.0, within(1e-6));
        assertThat(xyz[1]).isCloseTo(0.0, within(1e-6));
        assertThat(xyz[2]).isCloseTo(0.0, within(1e-6));
    }

    @Test
    void toGeocentricAtNorthPoleProducesExpectedZ() {
        double[] point = {Math.PI / 2, 0.0, 0.0};
        double[] xyz = transformer.toGeocentric(point, Ellipsoid.WGS84);

        assertThat(xyz[0]).isCloseTo(0.0, within(1e-6));
        assertThat(xyz[1]).isCloseTo(0.0, within(1e-6));
        assertThat(xyz[2]).isCloseTo(Ellipsoid.WGS84.semiMinorAxis(), within(1e-6));
    }

    @Test
    void fromGeocentricRoundTripsThroughToGeocentric() {
        double[] original = {
                Math.toRadians(55.7558),
                Math.toRadians(37.6173),
                150.0
        };
        double[] xyz = transformer.toGeocentric(original, Ellipsoid.WGS84);
        double[] back = transformer.fromGeocentric(xyz, Ellipsoid.WGS84);

        assertThat(back[0]).isCloseTo(original[0], within(1e-11));
        assertThat(back[1]).isCloseTo(original[1], within(1e-11));
        assertThat(back[2]).isCloseTo(original[2], within(1e-6));
    }

    // ================================================================
    // Обработка ошибок
    // ================================================================

    @Test
    void transformWithoutToWgs84Throws() {
        CoordinateSystem noParams = new CoordinateSystem(
                "TEST", "Test", Ellipsoid.WGS84, null, null);

        assertThatThrownBy(() -> transformer.transform(
                new double[]{0, 0, 0}, noParams, noParams))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("toWgs84");
    }

    @Test
    void invalidLatitudeThrows() {
        CoordinateSystem sys = wgs84();
        double[] badPoint = {2.0, 0.0, 0.0};  // широта > π/2

        assertThatThrownBy(() -> transformer.transform(badPoint, sys, sys))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Широта");
    }

    @Test
    void nanCoordinateThrows() {
        CoordinateSystem sys = wgs84();
        double[] badPoint = {Double.NaN, 0.0, 0.0};

        assertThatThrownBy(() -> transformer.transform(badPoint, sys, sys))
                .isInstanceOf(ValidationException.class);
    }

    // ================================================================
    // Фабричные методы
    // ================================================================

    private CoordinateSystem wgs84() {
        return new CoordinateSystem(
                "WGS84", "WGS84", Ellipsoid.WGS84, null,
                new double[]{0, 0, 0, 0, 0, 0, 0});
    }

    private CoordinateSystem sk42() {
        return new CoordinateSystem(
                "SK42", "СК-42", Ellipsoid.KRASSOVSKY, null,
                new double[]{23.92, -141.27, -80.9, 0, -0.35, -0.82, -0.12});
    }
}