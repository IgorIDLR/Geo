package ru.geodesy.adjust.adjustment.internal.solver;

import org.ejml.simple.SimpleMatrix;

/**
 * Cholesky с регуляризацией Тихонова:
 * N_reg = N + λ·I, где λ = 1e-6 · mean(diag(N)).
 * Используется для больших свободных сетей.
 */
public final class RegularizedCholeskySolver implements Solver {

    private static final double REGULARIZATION_FACTOR = 1e-6;

    private final DenseCholeskySolver delegate = new DenseCholeskySolver();

    @Override
    public SimpleMatrix solve(SimpleMatrix N, SimpleMatrix b) {
        return delegate.solve(regularize(N), b);
    }

    @Override
    public SimpleMatrix invert(SimpleMatrix N) {
        return delegate.invert(regularize(N));
    }

    private SimpleMatrix regularize(SimpleMatrix N) {
        int n = N.numRows();
        double diagMean = 0;
        for (int i = 0; i < n; i++) diagMean += N.get(i, i);
        diagMean /= n;

        SimpleMatrix Nd = N.copy();
        double lambda = REGULARIZATION_FACTOR * Math.abs(diagMean);
        for (int i = 0; i < n; i++) {
            Nd.set(i, i, N.get(i, i) + lambda);
        }
        return Nd;
    }
}