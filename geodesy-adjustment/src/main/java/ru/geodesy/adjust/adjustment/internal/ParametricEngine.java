package ru.geodesy.adjust.adjustment.internal;

import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.adjustment.api.AdjustmentEngine;
import ru.geodesy.adjust.adjustment.api.AdjustmentOptions;
import ru.geodesy.adjust.adjustment.api.AdjustmentResult;
import ru.geodesy.adjust.adjustment.internal.assembler.NormalEquationAssembler;
import ru.geodesy.adjust.adjustment.internal.constraints.FreeNetworkConstraints;
import ru.geodesy.adjust.adjustment.internal.solver.Solver;
import ru.geodesy.adjust.adjustment.internal.solver.SolverFactory;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;

import java.util.List;

/**
 * Параметрический способ уравнивания с демпфированием Левенберга-Марквардта.
 * <p>
 * Реализация:
 * <ol>
 *   <li>Итеративный процесс: сборка N, добавление λ к диагонали,
 *       решение системы, обновление X</li>
 *   <li>Адаптивное изменение λ в зависимости от maxDelta</li>
 *   <li>Сохранение последней N для вычисления Q без повторной сборки</li>
 *   <li>Явный флаг {@code converged} — не путать сходимость
 *       с исчерпанием итераций</li>
 * </ol>
 */
@Slf4j
@Component
public class ParametricEngine extends AbstractEngine {

    private final SolverFactory solverFactory;

    public ParametricEngine(NormalEquationAssembler assembler,
                            SolverFactory solverFactory) {
        super(assembler);
        this.solverFactory = solverFactory;
    }

    /**
     * Публичный фасад для использования через интерфейс AdjustmentEngine.
     */
    public AdjustmentEngine asEngine() {
        return new Adapter(this);
    }

    /**
     * Основной алгоритм уравнивания.
     */
    public AdjustmentResult adjustInternal(Network network, double[] variances,
                                           AdjustmentOptions options) {
        long start = System.currentTimeMillis();

        ParameterSet params = ParameterSet.fromPoints(network.points());
        double[] X = params.getInitial();
        int n = X.length;
        boolean isFree = network.networkType().isFree();
        Solver solver = solverFactory.create(n, network.networkType());

        double lambda = 1e-3;
        boolean converged = false;
        int iter = 0;

        // Сохраняем последнюю нормальную матрицу для Q
        SimpleMatrix lastN = null;

        for (; iter < options.maxIterations(); iter++) {
            // 1. Сборка нормальных уравнений
            var eq = assembler.assemble(network, params, X);
            SimpleMatrix N = eq.N();
            SimpleMatrix b = eq.b();

            // 2. Внутренние ограничения для свободных сетей
            if (isFree) {
                N = FreeNetworkConstraints.apply(N, network);
            }

            // 3. Демпфирование Левенберга-Марквардта
            if (options.applyDamping()) {
                SimpleMatrix Nd = N.copy();
                for (int i = 0; i < n; i++) {
                    Nd.set(i, i, N.get(i, i) * (1 + lambda));
                }
                N = Nd;
            }

            // 4. Решение системы N·Δx = b
            SimpleMatrix delta = solver.solve(N, b);

            // 5. Обновление X
            double maxDelta = 0;
            for (int i = 0; i < n; i++) {
                double d = delta.get(i, 0);
                X[i] += d;
                maxDelta = Math.max(maxDelta, Math.abs(d));
            }

            // 6. Адаптация λ
            if (maxDelta < options.convergenceEps() * 10) {
                lambda = Math.max(lambda * 0.1, 1e-6);
            } else {
                lambda = Math.min(lambda * 10, 1e6);
            }

            lastN = N;

            log.debug("Parametric iter {}: maxDelta = {}, λ = {}",
                    iter + 1, maxDelta, lambda);

            // 7. Проверка сходимости
            if (maxDelta < options.convergenceEps()) {
                converged = true;
                iter++;
                break;
            }
        }

        if (!converged) {
            log.warn("Параметрическое уравнивание не сошлось за {} итераций "
                            + "(последняя поправка > {}), σ₀ может быть некорректна",
                    options.maxIterations(), options.convergenceEps());
        }

        // 8. Ковариационная матрица через сохранённую N
        SimpleMatrix Q = null;
        if (options.computeCovariance() && lastN != null) {
            try {
                Q = solver.invert(lastN);
            } catch (Exception e) {
                log.warn("Не удалось обратить N для ковариации: {}", e.getMessage());
                Q = null;
            }
        }

        // 9. СКО единицы веса
        double refErr = computeReferenceError(network, X, variances);

        // 10. Сборка результата
        return buildResult(network, X, Q, refErr, iter, converged,
                List.of(), "PARAMETRIC",
                System.currentTimeMillis() - start);
    }

    /**
     * Статический адаптер без захвата внешнего класса — предотвращает
     * утечку памяти.
     */
    private static final class Adapter implements AdjustmentEngine {

        private final ParametricEngine delegate;

        Adapter(ParametricEngine delegate) {
            this.delegate = delegate;
        }

        @Override
        public AdjustmentResult adjust(Network network, double[] initialVar,
                                       AdjustmentOptions options) {
            return delegate.adjustInternal(network, initialVar, options);
        }

        @Override
        public String methodName() {
            return "PARAMETRIC";
        }
    }
}