package ru.geodesy.adjust.transformation;

import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.transformation.internal.Ellipsoid;
import ru.geodesy.adjust.transformation.internal.TransformationParameters;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransformationParametersTest {

    @Test
    void ellipsoidConstantsAreCorrect() {
        assertThat(Ellipsoid.WGS84.semiMajorAxis()).isEqualTo(6378137.0);
        assertThat(Ellipsoid.KRASSOVSKY.semiMajorAxis()).isEqualTo(6378245.0);

        assertThat(Ellipsoid.WGS84.flattening()).isCloseTo(
                1.0 / 298.257223563, within(1e-12));
    }

    @Test
    void ellipsoidComputedPropertiesAreConsistent() {
        double a = Ellipsoid.WGS84.semiMajorAxis();
        double b = Ellipsoid.WGS84.semiMinorAxis();

        // Малая полуось меньше большой
        assertThat(b).isLessThan(a);

        // Эксцентриситет в квадрате
        double e2 = Ellipsoid.WGS84.eccentricitySquared();
        assertThat(e2).isCloseTo(0.00669438, within(1e-6));
    }

    @Test
    void meanRadiusIsBetweenSemiAxes() {
        double a = Ellipsoid.WGS84.semiMajorAxis();
        double b = Ellipsoid.WGS84.semiMinorAxis();
        double mean = Ellipsoid.WGS84.meanRadius();

        assertThat(mean).isBetween(b, a);
    }

    @Test
    void systemDefinitionsCanBePopulated() {
        TransformationParameters params = new TransformationParameters();
        params.setDefaultMethod("HELMERT");

        Map<String, TransformationParameters.SystemDefinition> systems =
                new HashMap<>();

        var wgs84 = new TransformationParameters.SystemDefinition();
        wgs84.setId("WGS84");
        wgs84.setName("WGS84");

        var datum = new TransformationParameters.DatumDefinition();
        datum.setSemiMajor(6378137.0);
        datum.setFlattening(1.0 / 298.257223563);
        wgs84.setDatum(datum);
        wgs84.setToWgs84(new double[]{0, 0, 0, 0, 0, 0, 0});

        systems.put("WGS84", wgs84);
        params.setSystems(systems);

        assertThat(params.getDefaultMethod()).isEqualTo("HELMERT");
        assertThat(params.getSystems()).containsKey("WGS84");
        assertThat(params.getSystems().get("WGS84").getDatum().getSemiMajor())
                .isEqualTo(6378137.0);
    }

    private static org.assertj.core.data.Offset<Double> within(double d) {
        return org.assertj.core.data.Offset.offset(d);
    }
}