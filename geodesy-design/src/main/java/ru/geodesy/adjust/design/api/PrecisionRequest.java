package ru.geodesy.adjust.design.api;

import ru.geodesy.adjust.model.Network;
import ru.geodesy.common.Preconditions;

/**
 * Запрос на прямую задачу — оценку ожидаемой точности без фактического
 * уравнивания.
 * <p>
 * Использует геометрию сети и априорные веса измерений для расчёта
 * ковариационной матрицы параметров Q = (AᵀPA)⁻¹.
 */
public record PrecisionRequest(
        Network network,
        double[] variances,
        double sigma0
) {

    public PrecisionRequest {
        Preconditions.requireNonNull(network, "precision.network");
        Preconditions.requireNonNull(variances, "precision.variances");
        Preconditions.requirePositive(sigma0, "precision.sigma0");

        if (variances.length != network.measurements().size()) {
            throw new IllegalArgumentException(
                    "Число дисперсий (" + variances.length
                            + ") не совпадает с числом измерений ("
                            + network.measurements().size() + ")");
        }
        variances = variances.clone();
    }

    public static PrecisionRequest of(Network network, double[] variances) {
        return new PrecisionRequest(network, variances, network.sigma0());
    }
}