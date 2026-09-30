package ru.geodesy.adjust.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.geodesy.common.ValidationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstrumentParametersTest {

    private InstrumentParameters params;

    @BeforeEach
    void setup() {
        params = new InstrumentParameters();
        Map<String, InstrumentParameters.InstrumentProps> map = new HashMap<>();

        var tacheo = new InstrumentParameters.InstrumentProps();
        tacheo.setAngleSigma(3.0);
        tacheo.setDistanceSigmaMm(2.0);
        tacheo.setDistanceSigmaPpm(2.0);
        map.put("tacheometer-3s", tacheo);

        var level = new InstrumentParameters.InstrumentProps();
        level.setHeightSigmaKm(0.7);
        map.put("level-h2", level);

        var gnss = new InstrumentParameters.InstrumentProps();
        gnss.setHorizontalSigmaMm(8.0);
        gnss.setVerticalSigmaMm(15.0);
        map.put("gnss-l1", gnss);

        params.setProps(map);
    }

    @Test
    void requireReturnsConfiguredProps() {
        var p = params.require("tacheometer-3s");
        assertThat(p.getAngleSigma()).isEqualTo(3.0);
        assertThat(p.isAngleCapable()).isTrue();
        assertThat(p.isDistanceCapable()).isTrue();
        assertThat(p.isLevelCapable()).isFalse();
    }

    @Test
    void requireThrowsForUnknown() {
        assertThatThrownBy(() -> params.require("unknown"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requireThrowsForBlank() {
        assertThatThrownBy(() -> params.require(""))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getOrNullReturnsNullForMissing() {
        assertThat(params.getOrNull("unknown")).isNull();
        assertThat(params.getOrNull(null)).isNull();
    }

    @Test
    void classificationIsCorrect() {
        assertThat(params.require("level-h2").isLevelCapable()).isTrue();
        assertThat(params.require("gnss-l1").isGnssCapable()).isTrue();
    }

    @Test
    void containsWorks() {
        assertThat(params.contains("tacheometer-3s")).isTrue();
        assertThat(params.contains("missing")).isFalse();
        assertThat(params.contains(null)).isFalse();
    }
}