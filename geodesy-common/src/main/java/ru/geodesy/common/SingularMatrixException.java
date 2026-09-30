package ru.geodesy.common;

import java.io.Serial;

/**
 * Нормальная или ковариационная матрица вырождена или плохо
 * обусловлена. Возможные причины:
 * <ul>
 *   <li>Дефект конфигурации сети (недостаточное число измерений).</li>
 *   <li>Дефект данных (свободная сеть без внутренних ограничений).</li>
 *   <li>Плохая геометрия (коллинеарные направления).</li>
 * </ul>
 */
public final class SingularMatrixException extends GeodesyException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final double conditionNumber;

    public SingularMatrixException(String message, double conditionNumber) {
        super(message);
        this.conditionNumber = conditionNumber;
    }

    public SingularMatrixException(String message) {
        this(message, Double.NaN);
    }

    public double getConditionNumber() {
        return conditionNumber;
    }
}