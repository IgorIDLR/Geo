package ru.geodesy.adjust.adjustment.internal.update;

import org.ejml.simple.SimpleMatrix;

/**
 * UD-фильтр Bierman — численно устойчивое обновление Q.
 * Гарантирует сохранение положительной определённости.
 */
public final class UdUpdater {
    private UdUpdater() {}

    public static SimpleMatrix update(SimpleMatrix Q, SimpleMatrix aq, double N) {
        int n = Q.numRows();
        double[][] U = new double[n][n];
        double[] D = new double[n];
        udDecompose(Q, U, D);

        double[] f = new double[n];
        for (int i = 0; i < n; i++) f[i] = aq.get(i, 0) / N;

        double alpha = N;
        for (int j = n - 1; j >= 0; j--) {
            double Dj = D[j];
            double fj = f[j];
            double beta = alpha + fj * fj * Dj;
            if (Math.abs(beta) < 1e-15) beta = 1e-15;
            double DjNew = Dj * alpha / beta;
            double gamma = alpha / beta;

            for (int i = 0; i < j; i++) {
                double Uij = U[i][j];
                U[i][j] = Uij - fj * f[i] * Dj * gamma;
                f[i] = f[i] + fj * Uij;
            }
            D[j] = DjNew;
            alpha = beta;
        }
        return udReconstruct(U, D);
    }

    private static void udDecompose(SimpleMatrix Q, double[][] U, double[] D) {
        int n = Q.numRows();
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                M[i][j] = Q.get(i, j);
            }
        }
        for (int j = n - 1; j >= 0; j--) {
            D[j] = M[j][j];
            U[j][j] = 1.0;
            if (Math.abs(D[j]) < 1e-15) D[j] = 1e-15;
            for (int i = 0; i < j; i++) {
                U[i][j] = M[i][j] / D[j];
                for (int k = 0; k < j; k++) {
                    M[i][k] -= U[i][j] * U[k][j] * D[j];
                }
            }
        }
    }

    private static SimpleMatrix udReconstruct(double[][] U, double[] D) {
        int n = U.length;
        SimpleMatrix Q = new SimpleMatrix(n, n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = 0;
                for (int k = i; k < n; k++) {
                    sum += U[i][k] * U[j][k] * D[k];
                }
                Q.set(i, j, sum);
                Q.set(j, i, sum);
            }
        }
        return Q;
    }
}