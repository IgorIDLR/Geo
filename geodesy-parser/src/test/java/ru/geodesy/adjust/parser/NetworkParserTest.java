package ru.geodesy.adjust.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.NetworkType;
import ru.geodesy.common.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class NetworkParserTest {

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
    void parsesSimpleLevelingNetwork() {
        String json = """
                {
                  "points": [
                    {"id": "A", "fixed": true, "z": 100.0},
                    {"id": "B", "fixed": false}
                  ],
                  "measurements": [
                    {"type": "LEVELING", "from": "A", "to": "B", "observed": 1.5}
                  ],
                  "networkType": "FIXED",
                  "sigma0": 1.0,
                  "dimension": 1
                }
                """;

        Network net = parser.parse(json);

        assertThat(net.points()).hasSize(2);
        assertThat(net.measurements()).hasSize(1);
        assertThat(net.networkType()).isEqualTo(NetworkType.fixed());
        assertThat(net.sigma0()).isCloseTo(1.0, within(1e-12));
        assertThat(net.dimension()).isEqualTo(1);
    }

    @Test
    void parsesPlanarNetworkWithAngle() {
        String json = """
                {
                  "points": [
                    {"id": "A", "fixed": true, "x": 0.0, "y": 0.0},
                    {"id": "B", "fixed": true, "x": 100.0, "y": 0.0},
                    {"id": "C", "fixed": false}
                  ],
                  "measurements": [
                    {"type": "ANGLE", "left": "A", "top": "B", "right": "C",
                     "observed": "45°30′00.0″"},
                    {"type": "DISTANCE", "from": "B", "to": "C", "observed": 100.0}
                  ],
                  "networkType": "FIXED"
                }
                """;

        Network net = parser.parse(json);

        assertThat(net.points()).hasSize(3);
        assertThat(net.measurements()).hasSize(2);
    }

    @Test
    void freeNetworkIsRecognized() {
        String json = """
                {
                  "points": [{"id": "A", "fixed": false}, {"id": "B", "fixed": false}],
                  "measurements": [
                    {"type": "DISTANCE", "from": "A", "to": "B", "observed": 100.0}
                  ],
                  "networkType": "FREE"
                }
                """;

        Network net = parser.parse(json);
        assertThat(net.networkType()).isEqualTo(NetworkType.free2D());
    }

    @Test
    void throwsForMissingPoints() {
        String json = """
                {
                  "measurements": []
                }
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void throwsForUnknownPointReference() {
        String json = """
                {
                  "points": [{"id": "A", "fixed": true, "x": 0, "y": 0}],
                  "measurements": [
                    {"type": "DISTANCE", "from": "A", "to": "UNKNOWN", "observed": 100.0}
                  ]
                }
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void throwsForInvalidJson() {
        assertThatThrownBy(() -> parser.parse("{invalid json"))
                .isInstanceOf(ParserException.class);
    }
}