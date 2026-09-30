package ru.geodesy.adjust.session.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Service;
import ru.geodesy.adjust.adjustment.api.AdjustmentEngine;
import ru.geodesy.adjust.adjustment.api.AdjustmentOptions;
import ru.geodesy.adjust.adjustment.api.AdjustmentRequest;
import ru.geodesy.adjust.adjustment.api.AdjustmentResult;
import ru.geodesy.adjust.adjustment.internal.ParametricEngine;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.session.api.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Реализация сервиса сессий.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRegistry registry;
    private final SessionProperties props;
    private final PatchValidator validator;
    private final AdjustmentEngineFactory engineFactory;

    @Override
    public SessionId create(AdjustmentRequest request) {
        Network network = engineFactory.parse(request);
        ParameterSet params = ParameterSet.fromPoints(network.points());
        double[] variances = engineFactory.initialVariances(network, request);

        SessionId id = SessionId.generate();
        AdjustmentSession session = registry.create(id, network, params, variances);

        // Первичное уравнивание
        fullRecalculate(session, request);
        log.info("Создана сессия {} с {} измерениями", id, network.measurements().size());
        return id;
    }

    @Override
    public SessionState get(SessionId id) {
        AdjustmentSession s = requireSession(id);
        return new SessionState(
                s.getId(), s.getCreatedAt(), s.getLastAccessAt(),
                s.getNetwork().measurements().size(),
                s.getNetwork().points().size(),
                s.getReferenceError(),
                s.getVersion() > 0,
                s.getVersion(),
                buildCoordinates(s),
                s.getLastResult()
        );
    }

    @Override
    public PatchResult patch(SessionId id, List<MeasurementPatch> patches) {
        long start = System.currentTimeMillis();
        AdjustmentSession session = requireSession(id);

        // 1. Валидация
        List<String> errors = validator.validate(session.getNetwork(), patches);
        if (!errors.isEmpty()) {
            return new PatchResult(false, 0, patches.size(), errors,
                    session.getReferenceError(), 0,
                    System.currentTimeMillis() - start, false);
        }

        // 2. Проверка порога
        if (patches.size() > props.getMaxIncrementalPatches()) {
            log.info("Патчей {} > {}, полный пересчёт", patches.size(),
                    props.getMaxIncrementalPatches());
            session.pushHistory();
            fullRecalculate(session, null);
            return new PatchResult(true, patches.size(), 0, List.of(),
                    session.getReferenceError(), session.getIterations(),
                    System.currentTimeMillis() - start, true);
        }

        // 3. Принудительная ресинхронизация каждые N патчей
        if (session.needsResync(props.getResyncInterval())) {
            log.info("Принудительная ресинхронизация Q после {} патчей",
                    session.getPatchesSinceResync());
            session.pushHistory();
            fullRecalculate(session, null);
            session.setPatchesSinceResync(0);
            return new PatchResult(true, patches.size(), 0, List.of(),
                    session.getReferenceError(), session.getIterations(),
                    System.currentTimeMillis() - start, true);
        }

        // 4. Инкрементальное обновление
        session.pushHistory();
        int applied = 0, rejected = 0;
        SimpleMatrix Q = session.getQ();
        SimpleMatrix b = session.getB();
        double[] X = session.getX();

        List<MeasurementPatch> ordered = orderPatches(patches);

        for (MeasurementPatch p : ordered) {
            boolean ok = switch (p) {
                case MeasurementPatch.Add a -> {
                    var m = engineFactory.parseSingleMeasurement(
                            a.measurementJson(), session);
                    yield IncrementalUpdater.add(Q, b, X, m, session.getParams());
                }
                case MeasurementPatch.Update u -> {
                    var oldM = session.getNetwork().measurements().get(u.index());
                    var newM = engineFactory.parseSingleMeasurement(
                            u.measurementJson(), session);
                    yield IncrementalUpdater.update(Q, b, X, oldM, newM,
                            session.getParams());
                }
                case MeasurementPatch.Remove r -> {
                    var m = session.getNetwork().measurements().get(r.index());
                    yield IncrementalUpdater.remove(Q, b, X, m, session.getParams());
                }
            };
            if (ok) applied++; else rejected++;
        }

        // 5. Уточняющая итерация (нелинейный случай)
        int iters = refineIterations(session, Q, b, X, 3);
        session.setVersion(session.getVersion() + 1);
        session.setPatchesSinceResync(session.getPatchesSinceResync() + patches.size());
        session.setIterations(iters);

        return new PatchResult(true, applied, rejected, List.of(),
                session.getReferenceError(), iters,
                System.currentTimeMillis() - start, false);
    }

    @Override
    public PatchResult preview(SessionId id, List<MeasurementPatch> patches) {
        AdjustmentSession session = requireSession(id);
        AdjustmentSession shadow = session.shallowCopy();
        // Применяем к теневой (без регистрации)
        patchInternal(shadow, patches);
        return new PatchResult(true, patches.size(), 0, List.of(),
                shadow.getReferenceError(), shadow.getIterations(), 0, false);
    }

    @Override
    public AdjustmentResult recalculate(SessionId id) {
        AdjustmentSession session = requireSession(id);
        session.pushHistory();
        return fullRecalculate(session, null);
    }

    @Override
    public AdjustmentResult rollback(SessionId id, int steps) {
        AdjustmentSession session = requireSession(id);
        if (!session.canRollback(steps)) {
            throw new SessionException("Недостаточно истории для отката на " + steps);
        }
        for (int i = 0; i < steps; i++) {
            HistoryEntry h = session.getHistory().pop();
            session.setX(h.X());
            session.setQ(h.Q());
            session.setN(h.N());
            session.setB(h.b());
            session.setReferenceError(h.referenceError());
            session.setIterations(h.iterations());
            session.setVersion(h.version());
            session.setPatchesSinceResync(h.patchesSinceResync());
        }
        return session.getLastResult();
    }

    @Override
    public void drop(SessionId id) {
        registry.remove(id);
    }

    // ================================================================
    // Вспомогательные
    // ================================================================

    private AdjustmentSession requireSession(SessionId id) {
        AdjustmentSession s = registry.get(id);
        if (s == null) throw new SessionException("Сессия не найдена: " + id);
        return s;
    }

    private AdjustmentResult fullRecalculate(AdjustmentSession session,
                                             AdjustmentRequest request) {
        Network network = session.getNetwork();
        ParameterSet params = session.getParams();
        double[] X = session.getX();

        // Полная сборка N и b
        SimpleMatrix N = engineFactory.assembleFull(network, params, X);
        SimpleMatrix b = engineFactory.assembleB(network, params, X);

        SimpleMatrix delta = N.solve(b);
        for (int i = 0; i < X.length; i++) X[i] += delta.get(i, 0);

        SimpleMatrix Q = N.invert();
        session.setQ(Q);
        session.setN(N);
        session.setB(b);
        session.setVersion(session.getVersion() + 1);
        session.setIterations(1);
        session.setPatchesSinceResync(0);

        return session.getLastResult();
    }

    private int refineIterations(AdjustmentSession session, SimpleMatrix Q,
                                 SimpleMatrix b, double[] X, int maxIter) {
        for (int iter = 0; iter < maxIter; iter++) {
            var eq = engineFactory.assembleEquations(
                    session.getNetwork(), session.getParams(), X);
            SimpleMatrix delta = eq.N().solve(eq.b());
            double maxDelta = 0;
            for (int i = 0; i < X.length; i++) {
                double d = delta.get(i, 0);
                X[i] += d;
                maxDelta = Math.max(maxDelta, Math.abs(d));
            }
            if (maxDelta < 1e-6) return iter + 1;
        }
        return maxIter;
    }

    private List<MeasurementPatch> orderPatches(List<MeasurementPatch> patches) {
        return patches.stream()
                .sorted((a, b) -> Integer.compare(priority(a), priority(b)))
                .toList();
    }

    private int priority(MeasurementPatch p) {
        return switch (p) {
            case MeasurementPatch.Remove r -> 0;
            case MeasurementPatch.Update u -> 1;
            case MeasurementPatch.Add a -> 2;
        };
    }

    private Map<String, double[]> buildCoordinates(AdjustmentSession s) {
        var params = s.getParams();
        var X = s.getX();
        var map = new LinkedHashMap<String, double[]>();
        for (var p : s.getNetwork().points()) {
            int base = params.indexOf(p.id(), 0);
            map.put(p.id(), new double[]{X[base], X[base+1], X[base+2]});
        }
        return map;
    }
}