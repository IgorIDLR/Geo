package ru.geodesy.adjust.config;

import org.junit.jupiter.api.Test;
import ru.geodesy.common.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanAccuracyClassTest {

    @Test
    void fromCodeWorks() {
        assertThat(PlanAccuracyClass.fromCode("class1")).isEqualTo(PlanAccuracyClass.CLASS1);
        assertThat(PlanAccuracyClass.fromCode("RAZR2")).isEqualTo(PlanAccuracyClass.RAZR2);
        assertThat(PlanAccuracyClass.fromCode("  Class3  ")).isEqualTo(PlanAccuracyClass.CLASS3);
    }

    @Test
    void fromCodeThrowsForUnknown() {
        assertThatThrownBy(() -> PlanAccuracyClass.fromCode("bad"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void fromCodeThrowsForNull() {
        assertThatThrownBy(() -> PlanAccuracyClass.fromCode(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void codeMatchesEnum() {
        assertThat(PlanAccuracyClass.CLASS1.code()).isEqualTo("class1");
        assertThat(PlanAccuracyClass.RAZR1.code()).isEqualTo("razr1");
    }

    @Test
    void heightClassFromCodeWorks() {
        assertThat(HeightAccuracyClass.fromCode("class2")).isEqualTo(HeightAccuracyClass.CLASS2);
        assertThat(HeightAccuracyClass.fromCode("CLASS4")).isEqualTo(HeightAccuracyClass.CLASS4);
    }
}