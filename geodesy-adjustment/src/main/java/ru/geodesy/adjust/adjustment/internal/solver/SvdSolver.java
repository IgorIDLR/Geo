package ru.geodesy.adjust.adjustment.internal.solver;

import org.apache.commons.math3.linear.*;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.adjustment.api.AdjustmentException;

/**
 * Псевдообращение через SVD для вырожденных (свободных) сетей.
 */
public final class SvdSolver implements Solver {

    @Override
    public SimpleMatrix solve(SimpleMatrix N, SimpleMatrix b) {
        return invert(N).mult(b);
    }

    @Override
    public SimpleMatrix invert(SimpleMatrix N) {
        int n = N.numRows();
        DMatrixRMaj Ncopy = N.copy().getDDRM();

        LinearSolverDense<DMatrixRMaj> solver =
                LinearSolverFactory_DDRM.pseudoInverse(true);

        if (!solver.setA(Ncopy)) {
            throw new AdjustmentException(
                    "SVD-псевдообращение не удалось");
        }

        DMatrixRMaj Q = new DMatrixRMaj(n, n);
        solver.invert(Q);
        return SimpleMatrix.wrap(Q);
    }

    private RealMatrix toRealMatrix(SimpleMatrix N) {
        int n = N.numRows();
        double[][] data = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                data[i][j] = N.get(i, j);
            }
        }
        return new Array2DRowRealMatrix(data);
    }
}