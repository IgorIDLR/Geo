package ru.geodesy.adjust.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class MeasurementParserTest {

    private MeasurementParser parser;
    private ParameterSet params;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        parser = new MeasurementParser();
        params = ParameterSet.fromPoints(List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0),
                new Point("C", false, 100.0, 100.0, 0.0)
        ));
    }

    @Test
    void parsesDistance() throws Exception {
        var node = mapper.readTree("""
                {"type":"DISTANCE","from":"A","to":"B","observed":100.0}
                """);
        var m = parser.parseMeasurement(node, params);
        assertThat(m).isInstanceOf(DistanceMeasurement.class);
        assertThat(m.getObserved()).isEqualTo(100.0);
    }

    @Test
    void parsesAngleFromDmsString() throws Exception {
        var node = mapper.readTree("""
                {"type":"ANGLE","left":"A","top":"B","right":"C",
                 "observed":"45°30′00.0″"}
                """);
        var m = parser.parseMeasurement(node, params);
        assertThat(m).isInstanceOf(HorizontalAngleMeasurement.class);
        assertThat(m.getObserved()).isCloseTo(Math.toRadians(45.5), within(1e-12));
    }

    @Test
    void parsesAngleFromObject() throws Exception {
        var node = mapper.readTree("""
                {"type":"ANGLE","left":"A","top":"B","right":"C",
                 "observed":{"d":45,"m":30,"s":0}}
                """);
        var m = parser.parseMeasurement(node, params);
        assertThat(m.getObserved()).isCloseTo(Math.toRadians(45.5), within(1e-12));
    }

    @Test
    void parsesAngleFromRadians() throws Exception {
        var node = mapper.readTree("""
                {"type":"ANGLE","left":"A","top":"B","right":"C",
                 "observed":0.7853981633974483}
                """);
        var m = parser.parseMeasurement(node, params);
        assertThat(m.getObserved()).isCloseTo(Math.PI / 4, within(1e-12));
    }

    @Test
    void parsesLeveling() throws Exception {
        var node = mapper.readTree("""
                {"type":"LEVELING","from":"A","to":"B","observed":1.234,"variance":4.0}
                """);
        var m = parser.parseMeasurement(node, params);
        assertThat(m).isInstanceOf(LevelingMeasurement.class);
        assertThat(m.getVariance()).isEqualTo(4.0);
    }

    @Test
    void throwsForUnknownType() throws Exception {
        var node = mapper.readTree("""
                {"type":"UNKNOWN","observed":1.0}
                """);
        assertThatThrownBy(() -> parser.parseMeasurement(node, params))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void throwsForMissingRequiredField() throws Exception {
        var node = mapper.readTree("""
                {"type":"DISTANCE","from":"A","observed":100.0}
                """);
        assertThatThrownBy(() -> parser.parseMeasurement(node, params))
                .isInstanceOf(ParserException.class);
    }
}