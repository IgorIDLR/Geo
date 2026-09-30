package ru.geodesy.adjust.model.measurement;

import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class DistanceMeasurementTest {

    private ParameterSet params() {
        return ParameterSet.fromPoints(List.of(
                new Point("A", false, 0.0, 0.0, 0.0),
                new Point("B", false, 0.0, 0.0, 0.0)
        ));
    }

    @Test
    void computeValueWorks() {
        var m = new DistanceMeasurement(5.0, 1e-6, "A", "B", params());
        double[] X = {0, 0, 0, 3, 4, 0};
        assertThat(m.computeValue(X)).isCloseTo(5.0, within(1e-12));
    }

    @Test
    void derivativesAreCorrect() {
        var m = new DistanceMeasurement(5.0, 1e-6, "A", "B", params());
        double[] X = {0, 0, 0, 3, 4, 0};
        double[] deriv = new double[6];
        m.derivatives(X, deriv);

        assertThat(deriv[3]).isCloseTo(0.6, within(1e-12));
        assertThat(deriv[4]).isCloseTo(0.8, within(1e-12));
        assertThat(deriv[0]).isCloseTo(-0.6, within(1e-12));
        assertThat(deriv[1]).isCloseTo(-0.8, within(1e-12));
    }

    @Test
    void sparseDerivativesMatchDense() {
        var m = new DistanceMeasurement(5.0, 1e-6, "A", "B", params());
        double[] X = {0, 0, 0, 3, 4, 0};
        double[] dense = new double[6];
        m.derivatives(X, dense);

        int[] idx = new int[8];
        double[] val = new double[8];
        int k = m.sparseDerivatives(X, idx, val);

        assertThat(k).isEqualTo(4);
        for (int i = 0; i < k; i++) {
            assertThat(val[i]).isCloseTo(dense[idx[i]], within(1e-12));
        }
    }

    @Test
    void zeroDistanceComputesToZero() {
        var m = new DistanceMeasurement(0.0, 1e-6, "A", "B", params());
        double[] X = {0, 0, 0, 0, 0, 0};
        assertThat(m.computeValue(X)).isEqualTo(0.0);
    }

    @Test
    void zeroDistanceDerivativesThrow() {
        var m = new DistanceMeasurement(0.0, 1e-6, "A", "B", params());
        double[] X = {0, 0, 0, 0, 0, 0};
        double[] deriv = new double[6];

        assertThatThrownBy(() -> m.derivatives(X, deriv))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Нулевое расстояние");   // ← ИСПРАВЛЕНО
    }

    @Test
    void zeroDistanceSparseDerivativesThrow() {
        var m = new DistanceMeasurement(0.0, 1e-6, "A", "B", params());
        double[] X = {0, 0, 0, 0, 0, 0};
        int[] idx = new int[8];
        double[] val = new double[8];

        assertThatThrownBy(() -> m.sparseDerivatives(X, idx, val))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Нулевое расстояние");   // ← ИСПРАВЛЕНО
    }

    @Test
    void negativeVarianceThrows() {
        assertThatThrownBy(() ->
                new DistanceMeasurement(5.0, -1e-6, "A", "B", params()))
                .isInstanceOf(RuntimeException.class);
    }
}