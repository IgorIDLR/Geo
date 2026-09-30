package ru.geodesy.adjust.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterSetTest {

    @Test
    void fromPointsBuildsCorrectIndices() {
        List<Point> points = List.of(
                new Point("A", true, 100.0, 200.0, 0.0),
                new Point("B", false, 300.0, 400.0, 0.0)
        );
        ParameterSet ps = ParameterSet.fromPoints(points);

        assertThat(ps.size()).isEqualTo(6);
        assertThat(ps.indexOf("A", 0)).isEqualTo(0);
        assertThat(ps.indexOf("A", 1)).isEqualTo(1);
        assertThat(ps.indexOf("A", 2)).isEqualTo(2);
        assertThat(ps.indexOf("B", 0)).isEqualTo(3);
        assertThat(ps.indexOf("B", 1)).isEqualTo(4);
        assertThat(ps.indexOf("B", 2)).isEqualTo(5);
    }

    @Test
    void indexOfUnknownPointThrows() {
        ParameterSet ps = ParameterSet.fromPoints(
                List.of(new Point("A", true, 0.0, 0.0, 0.0)));
        assertThatThrownBy(() -> ps.indexOf("X", 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("X");
    }

    @Test
    void invalidCoordinateThrows() {
        ParameterSet ps = ParameterSet.fromPoints(
                List.of(new Point("A", true, 0.0, 0.0, 0.0)));
        assertThatThrownBy(() -> ps.indexOf("A", 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void initialValuesCorrect() {
        ParameterSet ps = ParameterSet.fromPoints(List.of(
                new Point("A", true, 1.5, 2.5, 3.5)
        ));
        double[] init = ps.getInitial();
        assertThat(init).containsExactly(1.5, 2.5, 3.5);
    }
}