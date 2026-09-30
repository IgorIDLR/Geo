package ru.geodesy.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class AngleUtilsTest {

    @Test
    void degreesToRadiansWorks() {
        assertThat(AngleUtils.degreesToRadians(180))
                .isCloseTo(Math.PI, within(1e-12));
    }

    @Test
    void secondsToRadiansWorks() {
        double rad = AngleUtils.secondsToRadians(206264.806);
        assertThat(rad).isCloseTo(1.0, within(1e-5));
    }

    @Test
    void dmsToRadiansWorks() {
        double rad = AngleUtils.dmsToRadians(45, 30, 0);
        double expected = Math.toRadians(45.5);
        assertThat(rad).isCloseTo(expected, within(1e-12));
    }

    @Test
    void dmsWithNegativeDegrees() {
        double rad = AngleUtils.dmsToRadians(-45, 30, 0);
        double expected = Math.toRadians(-45.5);
        assertThat(rad).isCloseTo(expected, within(1e-12));
    }

    @Test
    void parseDmsStringWorks() {
        double rad = AngleUtils.parseDmsToRadians("45°30′15.0″");
        double expected = Math.toRadians(45 + 30.0/60 + 15.0/3600);
        assertThat(rad).isCloseTo(expected, within(1e-12));
    }

    @Test
    void parseDmsStringWithCommaDecimal() {
        double rad = AngleUtils.parseDmsToRadians("45°30′15,5″");
        double expected = Math.toRadians(45 + 30.0/60 + 15.5/3600);
        assertThat(rad).isCloseTo(expected, within(1e-12));
    }

    @Test
    void parseDmsRejectsNull() {
        assertThatThrownBy(() -> AngleUtils.parseDmsToRadians(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void radiansToDmsStringWorks() {
        double radians = Math.toRadians(45.5);
        String dms = AngleUtils.radiansToDmsString(radians);
        assertThat(dms).startsWith("45°30′");
    }

    @Test
    void normalizeTo0_2PiWorks() {
        assertThat(AngleUtils.normalizeTo0_2Pi(-Math.PI / 2))
                .isCloseTo(3 * Math.PI / 2, within(1e-12));
        assertThat(AngleUtils.normalizeTo0_2Pi(2 * Math.PI + 0.1))
                .isCloseTo(0.1, within(1e-12));
    }

    @Test
    void normalizeToPiWorks() {
        assertThat(AngleUtils.normalizeToPi(3 * Math.PI))
                .isCloseTo(Math.PI, within(1e-12));
        assertThat(AngleUtils.normalizeToPi(-3 * Math.PI / 2))
                .isCloseTo(Math.PI / 2, within(1e-12));
    }

    @Test
    void azimuthWorks() {
        assertThat(AngleUtils.azimuth(0, 0, 1, 0)).isCloseTo(0, within(1e-12));
        assertThat(AngleUtils.azimuth(0, 0, 0, 1)).isCloseTo(Math.PI / 2, within(1e-12));
        assertThat(AngleUtils.azimuth(0, 0, -1, 0)).isCloseTo(Math.PI, within(1e-12));
    }
}