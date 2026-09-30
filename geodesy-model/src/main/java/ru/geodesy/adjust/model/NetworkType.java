package ru.geodesy.adjust.model;

/**
 * Многомерное описание типа сети.
 * <p>
 * Определяет дефект ранга плановой и высотной частей, а также
 * наличие ориентации и масштаба. Используется для наложения
 * внутренних ограничений в свободных сетях.
 */
public record NetworkType(
        PlanarDefect planarDefect,
        VerticalDefect verticalDefect,
        boolean hasOrientation,
        boolean hasScale
) {

    /** Дефект ранга плановой части. */
    public enum PlanarDefect {
        /** Нет дефекта — все параметры определены. */
        ZERO,
        /** Свободен сдвиг (2 неизвестных). */
        SHIFT_XY,
        /** Свободен поворот (1 неизвестное). */
        ROTATION,
        /** Свободны сдвиг, поворот и масштаб (3 неизвестных). */
        FULL
    }

    /** Дефект ранга высотной части. */
    public enum VerticalDefect {
        /** Нет дефекта. */
        ZERO,
        /** Свободен сдвиг по Z. */
        SHIFT_Z
    }

    // ============================================================
    // Фабричные методы
    // ============================================================

    /** Полностью фиксированная сеть. */
    public static NetworkType fixed() {
        return new NetworkType(
                PlanarDefect.ZERO,
                VerticalDefect.ZERO,
                true,
                true);
    }

    /** Свободная плановая сеть (сдвиг + поворот). */
    public static NetworkType free2D() {
        return new NetworkType(
                PlanarDefect.FULL,
                VerticalDefect.ZERO,
                false,
                false);
    }

    /** Беспримычная сеть (известен масштаб, но не ориентация). */
    public static NetworkType unconnected2D() {
        return new NetworkType(
                PlanarDefect.ROTATION,
                VerticalDefect.ZERO,
                false,
                true);
    }

    /** Свободная пространственная сеть. */
    public static NetworkType free3D() {
        return new NetworkType(
                PlanarDefect.FULL,
                VerticalDefect.SHIFT_Z,
                false,
                false);
    }

    // ============================================================
    // Вычисляемые свойства
    // ============================================================

    /**
     * Число линейно-независимых ограничений для плановой части.
     */
    public int planarDefectRank() {
        return switch (planarDefect) {
            case ZERO -> 0;
            case SHIFT_XY -> 2;
            case ROTATION -> 1;
            case FULL -> 3;
        };
    }

    /**
     * Число линейно-независимых ограничений для высотной части.
     */
    public int verticalDefectRank() {
        return verticalDefect == VerticalDefect.ZERO ? 0 : 1;
    }

    /**
     * Общий дефект ранга.
     */
    public int totalDefectRank() {
        return planarDefectRank() + verticalDefectRank();
    }

    /**
     * Является ли сеть свободной (есть дефект ранга).
     */
    public boolean isFree() {
        return totalDefectRank() > 0;
    }
}