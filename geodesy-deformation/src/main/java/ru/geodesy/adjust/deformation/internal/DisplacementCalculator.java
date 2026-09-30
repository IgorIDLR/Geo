package ru.geodesy.adjust.deformation.internal;

import lombok.RequiredArgsConstructor;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.deformation.api.PointDisplacement;

import java.util.Map;

/**
 * Утилиты для интерпретации смещений.
 */
@Component
@RequiredArgsConstructor
public class DisplacementCalculator {

    private final DeformationProperties props;

    /**
     * Проверка значимости смещения по критерию Стьюдента.
     */
    public boolean isSignificant(double magnitude, double sigma, double significanceLevel) {
        if (sigma <= 0) return false;
        double t = magnitude / sigma;
        // Упрощённое: t > 2 для α = 0.05
        double tCrit = significanceLevel <= 0.05 ? 2.0 : 1.5;
        return t > tCrit;
    }

    /**
     * Вычисляет смещение точки с полной матрицей ковариации.
     */
    public PointDisplacement computeDisplacement(String pointId,
                                                 double[] X1, double[] X2,
                                                 SimpleMatrix Q1, SimpleMatrix Q2,
                                                 int baseIndex,
                                                 double sigma0) {
        double dx = X2[baseIndex] - X1[baseIndex];
        double dy = X2[baseIndex + 1] - X1[baseIndex + 1];
        double dz = X2[baseIndex + 2] - X1[baseIndex + 2];

        double q1xx = Q1.get(baseIndex, baseIndex);
        double q2xx = Q2.get(baseIndex, baseIndex);
        double sigmaDx = sigma0 * Math.sqrt(Math.max(q1xx + q2xx, 0));

        double q1yy = Q1.get(baseIndex + 1, baseIndex + 1);
        double q2yy = Q2.get(baseIndex + 1, baseIndex + 1);
        double sigmaDy = sigma0 * Math.sqrt(Math.max(q1yy + q2yy, 0));

        double q1zz = Q1.get(baseIndex + 2, baseIndex + 2);
        double q2zz = Q2.get(baseIndex + 2, baseIndex + 2);
        double sigmaDz = sigma0 * Math.sqrt(Math.max(q1zz + q2zz, 0));

        double magnitude = Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean significant = isSignificant(magnitude, sigmaDx,
                props.getSignificanceLevel());

        return new PointDisplacement(pointId, dx, dy, dz,
                sigmaDx, sigmaDy, sigmaDz,
                magnitude, significant, 1 - props.getSignificanceLevel());
    }
}