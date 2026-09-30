package ru.geodesy.adjust.adjustment.internal.constraints;

import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.NetworkType;
import ru.geodesy.adjust.model.Point;

/**
 * Внутренние ограничения для свободных сетей.
 * N_new = N + Gᵀ·G
 * <p>
 * Число строк G = defRank (НЕ число точек × defRank).
 * Каждая строка — одно ограничение, агрегируемое по всем
 * определяемым точкам.
 */
public final class FreeNetworkConstraints {

    private FreeNetworkConstraints() {}

    public static SimpleMatrix apply(SimpleMatrix N, Network network) {
        SimpleMatrix G = build(network);
        if (G.numRows() == 0) return N;
        return N.plus(G.transpose().mult(G));
    }

    private static SimpleMatrix build(Network network) {
        var pts = network.points();
        NetworkType t = network.networkType();
        int planar = t.planarDefectRank();
        int vertical = t.verticalDefectRank();
        int defRank = planar + vertical;
        int n = pts.size() * 3;

        if (defRank == 0) {
            return new SimpleMatrix(0, n);
        }

        // Центроид только по определяемым точкам с координатами
        double xc = 0, yc = 0, zc = 0;
        int count = 0;
        for (Point p : pts) {
            if (p.fixed()) continue;
            if (p.hasCoordinates()) {
                xc += p.x(); yc += p.y(); zc += p.z();
                count++;
            }
        }
        if (count == 0) return new SimpleMatrix(0, n);
        xc /= count; yc /= count; zc /= count;

        // G: defRank × n — по одной строке на ограничение
        SimpleMatrix G = SimpleMatrix.filled(defRank, n, 0);
        int row = 0;

        // Ограничение 1: сдвиг X — Σ dx_i = 0
        if (planar >= 1) {
            for (int i = 0; i < pts.size(); i++) {
                Point p = pts.get(i);
                if (p.fixed() || p.x() == null) continue;
                G.set(row, i * 3, 1.0);
            }
            row++;
        }

        // Ограничение 2: сдвиг Y
        if (planar >= 2) {
            for (int i = 0; i < pts.size(); i++) {
                Point p = pts.get(i);
                if (p.fixed() || p.y() == null) continue;
                G.set(row, i * 3 + 1, 1.0);
            }
            row++;
        }

        // Ограничение 3: поворот вокруг центроида
        if (planar >= 3) {
            for (int i = 0; i < pts.size(); i++) {
                Point p = pts.get(i);
                if (p.fixed() || p.x() == null || p.y() == null) continue;
                double dx = p.x() - xc;
                double dy = p.y() - yc;
                G.set(row, i * 3, -dy);
                G.set(row, i * 3 + 1, dx);
            }
            row++;
        }

        // Ограничение 4: сдвиг Z (для 3D)
        if (vertical >= 1) {
            for (int i = 0; i < pts.size(); i++) {
                Point p = pts.get(i);
                if (p.fixed() || p.z() == null) continue;
                G.set(row, i * 3 + 2, 1.0);
            }
            row++;
        }

        return G;
    }
}