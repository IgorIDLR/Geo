package ru.geodesy.adjust.adjustment.internal.solver;

import org.ejml.simple.SimpleMatrix;

/**
 * CG с диагональным предобусловливателем Якоби.
 * Умножение матрицы на вектор реализовано через прямой доступ к data[].
 */
public final class JacobiCgSolver implements Solver {

    private static final int MAX_ITER = 2000;
    private static final double TOL = 1e-10;

    @Override
    public SimpleMatrix solve(SimpleMatrix N, SimpleMatrix b) {
        int n = N.numRows();
        double[] data = N.getDDRM().data;

        double[] x = new double[n];
        double[] r = b.getDDRM().data.clone();
        double[] z = new double[n];
        double[] p = new double[n];
        double[] M = new double[n];

        for (int i = 0; i < n; i++) {
            double diag = data[i * n + i];
            M[i] = Math.abs(diag) > 1e-15 ? 1.0 / diag : 1.0;
        }
        for (int i = 0; i < n; i++) z[i] = r[i] * M[i];
        System.arraycopy(z, 0, p, 0, n);

        double rz = dot(r, z);
        double bNorm = Math.sqrt(dot(r, r));
        if (bNorm < 1e-15) return new SimpleMatrix(n, 1);

        double[] Ap = new double[n];

        for (int iter = 0; iter < MAX_ITER; iter++) {
            java.util.Arrays.fill(Ap, 0.0);
            for (int i = 0; i < n; i++) {
                int base = i * n;
                double sum = 0;
                for (int j = 0; j < n; j++) {
                    sum += data[base + j] * p[j];
                }
                Ap[i] = sum;
            }

            double pAp = dot(p, Ap);
            if (Math.abs(pAp) < 1e-30) break;

            double alpha = rz / pAp;
            for (int i = 0; i < n; i++) {
                x[i] += alpha * p[i];
                r[i] -= alpha * Ap[i];
            }

            double rNorm = Math.sqrt(dot(r, r));
            if (rNorm / bNorm < TOL) break;

            for (int i = 0; i < n; i++) z[i] = r[i] * M[i];
            double rzNew = dot(r, z);
            double beta = rzNew / rz;
            for (int i = 0; i < n; i++) p[i] = z[i] + beta * p[i];
            rz = rzNew;
        }

        SimpleMatrix result = new SimpleMatrix(n, 1);
        for (int i = 0; i < n; i++) result.set(i, 0, x[i]);
        return result;
    }

    @Override
    public SimpleMatrix invert(SimpleMatrix N) {
        int n = N.numRows();
        SimpleMatrix Q = new SimpleMatrix(n, n);
        for (int j = 0; j < n; j++) {
            SimpleMatrix e = new SimpleMatrix(n, 1);
            e.set(j, 0, 1.0);
            SimpleMatrix col = solve(N, e);
            for (int i = 0; i < n; i++) Q.set(i, j, col.get(i, 0));
        }
        return Q;
    }

    private double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }
}