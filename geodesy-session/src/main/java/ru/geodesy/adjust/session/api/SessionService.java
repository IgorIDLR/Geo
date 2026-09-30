package ru.geodesy.adjust.session.api;

import ru.geodesy.adjust.adjustment.api.AdjustmentRequest;
import ru.geodesy.adjust.adjustment.api.AdjustmentResult;

import java.util.List;

/**
 * Публичный API управления сессиями.
 */
public interface SessionService {

    /** Создать новую сессию на основе запроса. */
    SessionId create(AdjustmentRequest request);

    /** Получить текущее состояние сессии. */
    SessionState get(SessionId id);

    /** Применить патчи и пересчитать инкрементально. */
    PatchResult patch(SessionId id, List<MeasurementPatch> patches);

    /** Полный пересчёт сети. */
    AdjustmentResult recalculate(SessionId id);

    /** Оценка влияния патчей без применения. */
    PatchResult preview(SessionId id, List<MeasurementPatch> patches);

    /** Откатить последние N изменений. */
    AdjustmentResult rollback(SessionId id, int steps);

    /** Удалить сессию. */
    void drop(SessionId id);
}