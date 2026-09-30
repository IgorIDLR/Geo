package ru.geodesy.adjust.session.internal;

import org.ejml.simple.SimpleMatrix;

/**
 * Снимок состояния сессии для отката.
 * Хранит diff-изменения, а не полные копии (экономия памяти).
 */
record HistoryEntry(
        double[] X,
        SimpleMatrix Q,
        SimpleMatrix N,
        SimpleMatrix b,
        double referenceError,
        int iterations,
        int version,
        int patchesSinceResync
) {}