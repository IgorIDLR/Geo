package ru.geodesy.adjust.adjustment.internal.solver;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.adjustment.api.AdjustmentException;

/**
 * Плотный Cholesky-решатель через LinearSolverFactory_DDRM.
 * Корректный API EJML 0.43.
 */
public final class DenseCholeskySolver implements Solver {

    @Override
    public SimpleMatrix solve(SimpleMatrix N, SimpleMatrix b) {
        int n = N.numRows();

        // Копия — setA модифицирует входную матрицу
        DMatrixRMaj Ncopy = N.copy().getDDRM();
        DMatrixRMaj bcopy = b.copy().getDDRM();

        LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.chol(n);

        if (!solver.setA(Ncopy)) {
            throw new AdjustmentException(
                    "Cholesky-разложение не удалось: матрица не положительно определена");
        }

        DMatrixRMaj x = new DMatrixRMaj(n, 1);
        solver.solve(bcopy, x);
        return SimpleMatrix.wrap(x);
    }

    @Override
    public SimpleMatrix invert(SimpleMatrix N) {
        int n = N.numRows();
        DMatrixRMaj Ncopy = N.copy().getDDRM();

        LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.chol(n);
        if (!solver.setA(Ncopy)) {
            throw new AdjustmentException("Cholesky-разложение не удалось при обращении");
        }

        DMatrixRMaj Q = new DMatrixRMaj(n, n);
        solver.invert(Q);
        return SimpleMatrix.wrap(Q);
    }
}