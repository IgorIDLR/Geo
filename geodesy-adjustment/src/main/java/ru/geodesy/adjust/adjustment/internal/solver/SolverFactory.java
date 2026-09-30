package ru.geodesy.adjust.adjustment.internal.solver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.NetworkType;

/**
 * Фабрика решателей с адаптивным выбором по размеру и типу сети.
 */
@Slf4j
@Component
public class SolverFactory {

    private static final int DENSE_THRESHOLD = 5000;
    private static final int FREE_SVD_THRESHOLD = 10_000;

    public Solver create(int n, NetworkType type) {
        if (type.isFree()) {
            if (n < FREE_SVD_THRESHOLD) {
                log.debug("Решатель: SVD (свободная сеть, n = {})", n);
                return new SvdSolver();
            }
            log.debug("Решатель: регуляризованный Cholesky (свободная сеть, n = {})", n);
            return new RegularizedCholeskySolver();
        }
        if (n < DENSE_THRESHOLD) {
            log.debug("Решатель: Cholesky (n = {})", n);
            return new DenseCholeskySolver();
        }
        log.debug("Решатель: Jacobi-CG (n = {})", n);
        return new JacobiCgSolver();
    }
}