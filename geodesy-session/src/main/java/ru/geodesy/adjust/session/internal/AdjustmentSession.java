package ru.geodesy.adjust.session.internal;

import lombok.Getter;
import lombok.Setter;
import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.adjustment.api.AdjustmentResult;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.session.api.SessionId;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Внутреннее состояние одной сессии.
 */
@Getter
@Setter
class AdjustmentSession {

    private final SessionId id;
    private final Network network;
    private final ParameterSet params;
    private final double[] initialVariances;

    private double[] X;
    private SimpleMatrix Q;
    private SimpleMatrix N;
    private SimpleMatrix b;

    private double referenceError;
    private int iterations;
    private int version;
    private int patchesSinceResync;

    private final Instant createdAt = Instant.now();
    private Instant lastAccessAt = Instant.now();

    private final Deque<HistoryEntry> history = new ArrayDeque<>();
    private static final int MAX_HISTORY = 10;

    private AdjustmentResult lastResult;

    AdjustmentSession(SessionId id, Network network, ParameterSet params,
                      double[] initialVariances) {
        this.id = id;
        this.network = network;
        this.params = params;
        this.initialVariances = initialVariances.clone();
        this.X = params.getInitial();
    }

    void touch() {
        this.lastAccessAt = Instant.now();
    }

    void pushHistory() {
        history.push(new HistoryEntry(
                X.clone(),
                Q != null ? Q.copy() : null,
                N != null ? N.copy() : null,
                b != null ? b.copy() : null,
                referenceError, iterations, version, patchesSinceResync
        ));
        while (history.size() > MAX_HISTORY) history.removeLast();
    }

    boolean canRollback(int steps) {
        return history.size() >= steps;
    }

    boolean needsResync(int resyncInterval) {
        return patchesSinceResync >= resyncInterval;
    }
}