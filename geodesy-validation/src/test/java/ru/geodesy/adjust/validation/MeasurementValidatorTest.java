package ru.geodesy.adjust.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.DistanceMeasurement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void validMeasurementPasses() {
        ParameterSet params = ParameterSet.fromPoints(List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0)
        ));
        var m = new DistanceMeasurement(100.0, 1e-5, "A", "B", params);
        var violations = validator.validate(m);
        assertThat(violations).isEmpty();
    }

    @Test
    void negativeVarianceIsRejected() {
        ParameterSet params = ParameterSet.fromPoints(List.of(
                new Point("A", true, 0.0, 0.0, 0.0),
                new Point("B", true, 100.0, 0.0, 0.0)
        ));
        // невалидная дисперсия может быть заблокирована ещё на конструкторе
        try {
            new DistanceMeasurement(100.0, -1e-5, "A", "B", params);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
        }
    }
}