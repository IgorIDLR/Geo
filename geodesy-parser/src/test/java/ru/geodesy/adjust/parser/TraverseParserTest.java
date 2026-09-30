package ru.geodesy.adjust.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.model.Network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraverseParserTest {

    private NetworkParser parser;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = new ObjectMapper();
        PointParser pointParser = new PointParser();
        MeasurementParser measParser = new MeasurementParser();
        TraverseParser traverseParser = new TraverseParser(pointParser);
        parser = new NetworkParser(mapper, pointParser, measParser, traverseParser);
    }

    @Test
    void parsesTraverse() {
        String json = """
                {
                  "type": "TRAVERSE",
                  "initial": {
                    "startPoint": "P1",
                    "coordinates": [
                      {"id": "P1", "fixed": true, "x": 0.0, "y": 0.0},
                      {"id": "P2", "fixed": true, "x": 100.0, "y": 0.0}
                    ]
                  },
                  "legs": [
                    {"pointId": "P3", "angle": "150°00′00″", "distance": 100.0}
                  ]
                }
                """;

        Network net = parser.parse(json);

        assertThat(net.points()).hasSize(3);
        assertThat(net.measurements()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void throwsForMissingLegs() {
        String json = """
                {
                  "type": "TRAVERSE",
                  "initial": {
                    "startPoint": "P1",
                    "coordinates": [
                      {"id": "P1", "fixed": true, "x": 0.0, "y": 0.0},
                      {"id": "P2", "fixed": true, "x": 100.0, "y": 0.0}
                    ]
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("legs");
    }
}