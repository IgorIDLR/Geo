package ru.geodesy.adjust.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.geodesy.common.ValidationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeodeticTolerancesTest {

    private GeodeticTolerances tolerances;

    @BeforeEach
    void setup() {
        tolerances = new GeodeticTolerances();

        Map<String, GeodeticTolerances.PlanTolerance> plan = new HashMap<>();
        plan.put("class1", planTol(0.7, "1:1000000"));
        plan.put("class2", planTol(1.0, "1:250000"));
        plan.put("class3", planTol(2.0, "1:100000"));

        Map<String, GeodeticTolerances.HeightTolerance> height = new HashMap<>();
        height.put("class1", heightTol(0.8));
        height.put("class2", heightTol(2.0));

        tolerances.setPlan(plan);
        tolerances.setHeight(height);
    }

    @Test
    void requirePlanReturnsConfiguredTolerance() {
        var t = tolerances.requirePlan("class1");
        assertThat(t.getAngleSigma()).isCloseTo(0.7, within(1e-12));
        assertThat(t.relativeDistDenominator()).isCloseTo(1_000_000.0, within(1.0));
    }

    @Test
    void requirePlanUppercasesAndTrimsInput() {
        var t = tolerances.requirePlan("  CLASS2  ");
        assertThat(t.getAngleSigma()).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void requirePlanThrowsForUnknownClass() {
        assertThatThrownBy(() -> tolerances.requirePlan("razr2"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("не сконфигурирован");
    }

    @Test
    void requirePlanThrowsForInvalidCode() {
        assertThatThrownBy(() -> tolerances.requirePlan("unknown"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Неизвестный класс");
    }

    @Test
    void requireHeightReturnsConfiguredTolerance() {
        var t = tolerances.requireHeight("class2");
        assertThat(t.getHeightSigma()).isCloseTo(2.0, within(1e-12));
    }

    @Test
    void getPlanOrNullReturnsNullForUnknown() {
        assertThat(tolerances.getPlanOrNull("unknown")).isNull();
        assertThat(tolerances.getPlanOrNull(null)).isNull();
    }

    @Test
    void relativeDistDenominatorParsesCorrectly() {
        var t = planTol(0.7, "1:250000");
        assertThat(t.relativeDistDenominator()).isEqualTo(250_000.0);
    }

    @Test
    void relativeDistDenominatorThrowsForBadFormat() {
        var t = planTol(0.7, "250000");
        assertThatThrownBy(t::relativeDistDenominator)
                .isInstanceOf(ValidationException.class);
    }

    private static GeodeticTolerances.PlanTolerance planTol(double angleSigma, String rel) {
        var t = new GeodeticTolerances.PlanTolerance();
        t.setAngleSigma(angleSigma);
        t.setRelativeDist(rel);
        return t;
    }

    private static GeodeticTolerances.HeightTolerance heightTol(double sigma) {
        var t = new GeodeticTolerances.HeightTolerance();
        t.setHeightSigma(sigma);
        return t;
    }
}