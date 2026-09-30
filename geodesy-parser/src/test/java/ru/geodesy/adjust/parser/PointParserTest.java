package ru.geodesy.adjust.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointParserTest {

    private PointParser parser;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        parser = new PointParser();
    }

    @Test
    void parsesSimplePoint() throws Exception {
        var node = mapper.readTree("""
                {"id":"A","fixed":true,"x":100.0,"y":200.0,"z":0.0}
                """);
        var p = parser.parsePoint(node);
        assertThat(p.id()).isEqualTo("A");
        assertThat(p.fixed()).isTrue();
        assertThat(p.x()).isEqualTo(100.0);
        assertThat(p.y()).isEqualTo(200.0);
        assertThat(p.covariance()).isNull();
    }

    @Test
    void parsesIsotropicCovariance() throws Exception {
        var node = mapper.readTree("""
                {"id":"A","fixed":true,"x":0,"y":0,"sigmaCoord":0.005}
                """);
        var p = parser.parsePoint(node);
        assertThat(p.covariance()).isNotNull();
        assertThat(p.covariance()[0][0]).isCloseTo(0.000025, within(1e-12));
    }

    @Test
    void parsesDiagonalCovariance() throws Exception {
        var node = mapper.readTree("""
                {"id":"A","fixed":true,"x":0,"y":0,
                 "sigmaX":0.003,"sigmaY":0.004,"sigmaZ":0.005}
                """);
        var p = parser.parsePoint(node);
        assertThat(p.covariance()[0][0]).isCloseTo(0.000009, within(1e-12));
        assertThat(p.covariance()[1][1]).isCloseTo(0.000016, within(1e-12));
        assertThat(p.covariance()[2][2]).isCloseTo(0.000025, within(1e-12));
    }

    @Test
    void parsesFullCovariance() throws Exception {
        var node = mapper.readTree("""
                {"id":"A","fixed":true,"x":0,"y":0,
                 "covariance":[[1e-5,0,0],[0,2e-5,0],[0,0,3e-5]]}
                """);
        var p = parser.parsePoint(node);
        assertThat(p.covariance()[0][0]).isEqualTo(1e-5);
        assertThat(p.covariance()[1][1]).isEqualTo(2e-5);
    }

    @Test
    void throwsForMissingId() throws Exception {
        var node = mapper.readTree("""
                {"fixed":true,"x":0,"y":0}
                """);
        assertThatThrownBy(() -> parser.parsePoint(node))
                .isInstanceOf(RuntimeException.class);
    }

    private static org.assertj.core.data.Offset<Double> within(double d) {
        return org.assertj.core.data.Offset.offset(d);
    }
}