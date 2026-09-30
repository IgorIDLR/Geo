package ru.geodesy.adjust.design.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.geodesy.adjust.design.api.AccuracyDesigner;
import ru.geodesy.adjust.design.api.DesignRequest;
import ru.geodesy.adjust.design.api.DesignResult;
import ru.geodesy.common.DesignException;

/**
 * Реализация обратной задачи — проектирования точности.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccuracyDesignerImpl implements AccuracyDesigner {

    private final WeightAdjuster weightAdjuster;

    @Override
    public DesignResult design(DesignRequest request) {
        log.info("Проектирование точности: стратегия = {}", request.strategy());

        return switch (request.strategy()) {
            case ADJUST_WEIGHTS -> weightAdjuster.adjust(request);

            case ADD_MEASUREMENTS -> throw new DesignException(
                    "Стратегия ADD_MEASUREMENTS требует профиля full");

            case COMBINED -> {
                DesignResult weightsResult = weightAdjuster.adjust(request);
                if (weightsResult.achieved()) {
                    yield weightsResult;
                }
                throw new DesignException(
                        "Стратегия COMBINED требует профиля full");
            }
        };
    }
}