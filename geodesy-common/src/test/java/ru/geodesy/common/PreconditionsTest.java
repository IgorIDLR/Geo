package ru.geodesy.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreconditionsTest {

    @Test
    void requireNonNullReturnsValue() {
        String s = Preconditions.requireNonNull("abc", "test");
        assertThat(s).isEqualTo("abc");
    }

    @Test
    void requireNonNullThrowsForNull() {
        assertThatThrownBy(() -> Preconditions.requireNonNull(null, "test"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("test");
    }

    @Test
    void requirePositiveWorks() {
        assertThat(Preconditions.requirePositive(5.0, "value")).isEqualTo(5.0);
        assertThatThrownBy(() -> Preconditions.requirePositive(-1, "value"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Preconditions.requirePositive(0, "value"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requireFiniteRejectsNaN() {
        assertThatThrownBy(() -> Preconditions.requireFinite(Double.NaN, "x"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Preconditions.requireFinite(Double.POSITIVE_INFINITY, "x"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requireInRangeWorks() {
        assertThat(Preconditions.requireInRange(5, 0, 10, "x")).isEqualTo(5);
        assertThatThrownBy(() -> Preconditions.requireInRange(15, 0, 10, "x"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requireNonEmptyRejectsEmptyList() {
        assertThatThrownBy(() -> Preconditions.requireNonEmpty(List.of(), "items"))
                .isInstanceOf(ValidationException.class);
        assertThat(Preconditions.requireNonEmpty(List.of(1), "items")).hasSize(1);
    }
}