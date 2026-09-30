package ru.geodesy.adjust.deformation.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;

/**
 * MsplitCD — вариант Msplit с ограниченным датумом.
 * <p>
 * Фиксирует координаты выбранных "референсных" точек и оценивает
 * смещения остальных точек относительно этого датума.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MsplitCdEstimator {

    private final SquaredMsplitEstimator baseEstimator;
    private final DeformationProperties props;

    public SquaredMsplitEstimator.MsplitResult estimateWithDatum(
            Network network, ParameterSet params,
            double[] initialX, double[] variances, int datumDegree) {

        // 1. Применяем базовый Msplit
        SquaredMsplitEstimator.MsplitResult result =
                baseEstimator.estimate(network, params, initialX, variances);

        // 2. Корректируем под датум: сдвигаем S-гипотезу так,
        //    чтобы центроид устойчивых точек совпал с 0
        double[] XsAdjusted = adjustToDatum(result.XStable(), params, network, datumDegree);
        double[] XuAdjusted = adjustToDatum(result.XUnstable(), params, network, datumDegree);

        return new SquaredMsplitEstimator.MsplitResult(
                XsAdjusted, XuAdjusted, result.probabilities(),
                result.iterations(), result.converged());
    }

    /**
     * Удаляет из вектора X общий сдвиг, поворот и масштаб,
     * соответствующие датуму.
     */
    private double[] adjustToDatum(double[] X, ParameterSet params,
                                   Network network, int datumDegree) {
        double[] Xnew = X.clone();
        int count = 0;
        double sumX = 0, sumY = 0, sumZ = 0;

        for (Point p : network.points()) {
            if (p.fixed()) continue;
            int b = params.indexOf(p.id(), 0);
            sumX += Xnew[b];
            sumY += Xnew[b + 1];
            sumZ += Xnew[b + 2];
            count++;
        }

        if (count == 0) return Xnew;

        // Сдвиг центроида в 0
        double meanX = sumX / count;
        double meanY = sumY / count;
        double meanZ = sumZ / count;

        for (Point p : network.points()) {
            if (p.fixed()) continue;
            int b = params.indexOf(p.id(), 0);
            Xnew[b] -= meanX;
            Xnew[b + 1] -= meanY;
            if (datumDegree >= 3) Xnew[b + 2] -= meanZ;
        }
        return Xnew;
    }
}