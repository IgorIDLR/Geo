package ru.geodesy.adjust.transformation;

import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.transformation.internal.BasePointTransformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BasePointTransformationTest {

    private final BasePointTransformation transformer = new BasePointTransformation();

    // ================================================================
    // SHIFT_3 (3D)
    // ================================================================

    @Test
    void shiftEstimation3D() {
        double[][] from = {
                {100.0, 200.0, 0.0},
                {200.0, 200.0, 0.0},
                {200.0, 300.0, 0.0},
                {100.0, 300.0, 0.0}
        };
        double[][] to = {
                {105.0, 198.0, 2.0},
                {205.0, 198.0, 2.0},
                {205.0, 298.0, 2.0},
                {105.0, 298.0, 2.0}
        };

        var result = transformer.estimate(from, to,
                BasePointTransformation.Model.SHIFT_3);

        assertThat(result.parameters()[0]).isCloseTo(5.0, within(1e-9));
        assertThat(result.parameters()[1]).isCloseTo(-2.0, within(1e-9));
        assertThat(result.parameters()[2]).isCloseTo(2.0, within(1e-9));
        assertThat(result.rmsError()).isLessThan(1e-9);
    }

    // ================================================================
    // SHIFT_3 (2D)
    // ================================================================

    @Test
    void shiftEstimation2D() {
        double[][] from = {
                {100.0, 200.0},
                {200.0, 200.0},
                {200.0, 300.0}
        };
        double[][] to = {
                {105.0, 198.0},
                {205.0, 198.0},
                {205.0, 298.0}
        };

        var result = transformer.estimate(from, to,
                BasePointTransformation.Model.SHIFT_3);

        assertThat(result.parameters()[0]).isCloseTo(5.0, within(1e-9));
        assertThat(result.parameters()[1]).isCloseTo(-2.0, within(1e-9));
        assertThat(result.parameters()[2]).isCloseTo(0.0, within(1e-9));
        assertThat(result.rmsError()).isLessThan(1e-9);
    }

    // ================================================================
    // HELMERT_7
    // ================================================================

    @Test
    void helmertEstimationWithShiftOnly() {
        double[][] from = {
                {0.0, 0.0, 0.0},
                {100.0, 0.0, 0.0},
                {0.0, 100.0, 0.0},
                {100.0, 100.0, 0.0},
                {50.0, 50.0, 0.0}
        };

        double[][] to = new double[from.length][3];
        for (int i = 0; i < from.length; i++) {
            to[i][0] = from[i][0] + 10.0;
            to[i][1] = from[i][1] + 20.0;
            to[i][2] = from[i][2];
        }

        var result = transformer.estimate(from, to,
                BasePointTransformation.Model.HELMERT_7);

        assertThat(result.parameters()[0]).isCloseTo(10.0, within(1e-6));
        assertThat(result.parameters()[1]).isCloseTo(20.0, within(1e-6));
        assertThat(result.parameters()[2]).isCloseTo(0.0, within(1e-6));
        // Углы вращения и масштаб должны быть ~0
        assertThat(Math.abs(result.parameters()[3])).isLessThan(0.1);
        assertThat(Math.abs(result.parameters()[4])).isLessThan(0.1);
        assertThat(Math.abs(result.parameters()[5])).isLessThan(0.1);
        assertThat(Math.abs(result.parameters()[6])).isLessThan(0.1);
        assertThat(result.rmsError()).isLessThan(1e-6);
    }

    // ================================================================
    // AFFINE_12
    // ================================================================

    @Test
    void affineEstimationWithScaling() {
        // 5 некомпланарных точек
        double[][] from = {
                {0.0,   0.0,   0.0},
                {100.0, 0.0,   0.0},
                {0.0,   100.0, 0.0},
                {0.0,   0.0,   100.0},   // ← z ≠ 0
                {100.0, 100.0, 100.0}    // ← не в плоскости
        };

        // Аффинное: масштаб 2 по X, 3 по Y, 1 по Z
        double[][] to = new double[from.length][3];
        for (int i = 0; i < from.length; i++) {
            to[i][0] = from[i][0] * 2.0;
            to[i][1] = from[i][1] * 3.0;
            to[i][2] = from[i][2] * 1.0;
        }

        var result = transformer.estimate(from, to,
                BasePointTransformation.Model.AFFINE_12);

        // params[0] = a_xx = 2.0
        assertThat(result.parameters()[0]).isCloseTo(2.0, within(1e-9));
        // params[5] = a_yy = 3.0
        assertThat(result.parameters()[5]).isCloseTo(3.0, within(1e-9));
        // params[10] = a_zz = 1.0
        assertThat(result.parameters()[10]).isCloseTo(1.0, within(1e-9));
        assertThat(result.rmsError()).isLessThan(1e-9);
    }

    // ================================================================
    // Обработка ошибок
    // ================================================================

    @Test
    void tooFewPointsThrows() {
        double[][] from = {{0, 0, 0}, {1, 0, 0}};
        double[][] to = {{0, 0, 0}, {1, 0, 0}};

        assertThatThrownBy(() -> transformer.estimate(from, to,
                BasePointTransformation.Model.SHIFT_3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("минимум");
    }

    @Test
    void nullArraysThrows() {
        assertThatThrownBy(() -> transformer.estimate(null, null,
                BasePointTransformation.Model.SHIFT_3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void differentLengthsThrows() {
        double[][] from = {{0, 0, 0}, {1, 0, 0}, {2, 0, 0}};
        double[][] to = {{0, 0, 0}, {1, 0, 0}};

        assertThatThrownBy(() -> transformer.estimate(from, to,
                BasePointTransformation.Model.SHIFT_3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Разное число");
    }

    @Test
    void nanCoordinateThrows() {
        double[][] from = {
                {0, 0, 0}, {1, 0, 0}, {0, 1, 0}
        };
        double[][] to = {
                {0, 0, 0}, {1, 0, 0}, {Double.NaN, 1, 0}
        };

        assertThatThrownBy(() -> transformer.estimate(from, to,
                BasePointTransformation.Model.SHIFT_3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("некорректное");
    }
}