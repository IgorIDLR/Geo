package ru.geodesy.adjust.adjustment.internal.robust;

/**
 * Функция пересчёта веса по стандартизованной невязке.
 * Возвращает множитель (0..1) к исходному весу.
 */
public interface WeightFunction {
    double compute(double residual, double sigma);
}