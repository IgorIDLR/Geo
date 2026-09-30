package ru.geodesy.adjust.adjustment.internal.assembler;

import java.util.Objects;

/**
 * Ключ кэша производных: (номер итерации, id измерения).
 * Использует Objects.hash() для корректного распределения.
 */
public final class DerivativeCacheKey {

    private final long iteration;
    private final int measurementId;

    public DerivativeCacheKey(long iteration, int measurementId) {
        this.iteration = iteration;
        this.measurementId = measurementId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(iteration, measurementId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DerivativeCacheKey k)) return false;
        return iteration == k.iteration && measurementId == k.measurementId;
    }
}