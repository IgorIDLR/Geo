package ru.geodesy.adjust.transformation;

import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.transformation.api.CoordinateSystem;
import ru.geodesy.adjust.transformation.internal.Ellipsoid;
import ru.geodesy.adjust.transformation.internal.GaussKrugerTransformer;
import ru.geodesy.common.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GaussKrugerTransformerTest {

    private final GaussKrugerTransformer transformer = new GaussKrugerTransformer();

    // ================================================================
    // Прямая задача
    // ================================================================

    @Test
    void centralMeridianMapsToFalseEasting() {
        CoordinateSystem gk = gkZone7();
        double[] geo = {0.0, Math.toRadians(39.0), 0.0};
        double[] plane = transformer.projectToPlane(geo, gk);

        assertThat(plane[0]).isCloseTo(0.0, within(1.0));
        assertThat(plane[1]).isCloseTo(7_500_000.0, within(1.0));
    }

    @Test
    void knownCoordinateFromReference() {
        // B = 55.5°, L = 37.5°, зона 7 (CM = 39°)
        CoordinateSystem gk = gkZone7();
        double[] geo = {Math.toRadians(55.5), Math.toRadians(37.5), 0.0};
        double[] plane = transformer.projectToPlane(geo, gk);

        // X в диапазоне 6.0–6.3 млн м
        assertThat(plane[0]).isBetween(6_000_000.0, 6_300_000.0);
        // Y в диапазоне 7.3–7.45 млн м
        assertThat(plane[1]).isBetween(7_300_000.0, 7_450_000.0);
    }

    // ================================================================
    // Обратная задача
    // ================================================================

    @Test
    void roundTripProjection() {
        CoordinateSystem gk = gkZone7();
        double[] geo = {
                Math.toRadians(55.7558),
                Math.toRadians(37.6173),
                150.0
        };

        double[] plane = transformer.projectToPlane(geo, gk);
        double[] back = transformer.projectToGeographic(plane, gk);

        assertThat(back[0]).isCloseTo(geo[0], within(1e-9));
        assertThat(back[1]).isCloseTo(geo[1], within(1e-9));
        assertThat(back[2]).isCloseTo(geo[2], within(1e-6));
    }

    @Test
    void falseEastingIsRestoredOnRoundTrip() {
        CoordinateSystem gk = gkZone7();
        double[] plane = {6_150_000.0, 7_400_000.0, 100.0};
        double[] geo = transformer.projectToGeographic(plane, gk);
        double[] back = transformer.projectToPlane(geo, gk);

        // Допуск 1 мм — реалистичная точность для round-trip через ряды Крагера
        assertThat(back[0]).isCloseTo(plane[0], within(1e-3));
        assertThat(back[1]).isCloseTo(plane[1], within(1e-3));
        assertThat(back[2]).isCloseTo(plane[2], within(1e-6));
    }

    // ================================================================
    // Обработка ошибок
    // ================================================================

    @Test
    void poleLatitudeThrows() {
        CoordinateSystem gk = gkZone7();
        double[] pole = {Math.PI / 2, 0.0, 0.0};

        assertThatThrownBy(() -> transformer.projectToPlane(pole, gk))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("полюс");
    }

    // ================================================================
    // Сквозное преобразование через transform()
    // ================================================================

    @Test
    void transformFromGeographicToPlane() {
        CoordinateSystem geo = geographicSk42();
        CoordinateSystem gk = gkZone7();

        double[] point = {
                Math.toRadians(55.5),
                Math.toRadians(39.0),
                0.0
        };

        double[] result = transformer.transform(point, geo, gk);

        assertThat(result[0]).isGreaterThan(6_000_000.0);
        assertThat(result[1]).isCloseTo(7_500_000.0, within(100.0));
    }

    // ================================================================
    // Вспомогательные
    // ================================================================

    private CoordinateSystem gkZone7() {
        return new CoordinateSystem(
                "GK7", "Гаусса-Крюгера зона 7",
                Ellipsoid.KRASSOVSKY,
                new CoordinateSystem.Projection(
                        CoordinateSystem.Projection.ProjectionType.GAUSS_KRUGER,
                        39.0, 7_500_000.0, 0.0, 1.0),
                new double[]{23.92, -141.27, -80.9, 0, -0.35, -0.82, -0.12});
    }

    private CoordinateSystem geographicSk42() {
        return new CoordinateSystem(
                "SK42", "СК-42", Ellipsoid.KRASSOVSKY, null,
                new double[]{23.92, -141.27, -80.9, 0, -0.35, -0.82, -0.12});
    }
}