package ru.geodesy.adjust.design.internal;

import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.adjustment.internal.constraints.FreeNetworkConstraints;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.common.DesignException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * Расчёт ковариационной матрицы параметров Q = (AᵀPA)⁻¹.
 * <p>
 * ВАЖНО: параметрами являются ТОЛЬКО определяемые (не fixed) точки.
 * Фиксированные точки — это константы, а не неизвестные.
 */
@Slf4j
@Component
public class PrecisionCalculator {

    /**
     * Возвращает матрицу Q только по определяемым точкам.
     */
    public SimpleMatrix computeQ(Network network, double[] variances) {
        List<Point> allPoints = network.points();

        // Индексы определяемых точек
        List<Integer> freePointIndices = new ArrayList<>();
        for (int i = 0; i < allPoints.size(); i++) {
            if (!allPoints.get(i).fixed()) {
                freePointIndices.add(i);
            }
        }

        int nFree = freePointIndices.size() * 3;
        if (nFree == 0) {
            throw new DesignException(
                    "Нет определяемых точек — нечего анализировать");
        }

        // Отображение "полный индекс параметра" → "индекс в N"
        int nAll = allPoints.size() * 3;
        int[] fullToFree = new int[nAll];
        Arrays.fill(fullToFree, -1);
        for (int k = 0; k < freePointIndices.size(); k++) {
            int pi = freePointIndices.get(k);
            fullToFree[pi * 3]     = k * 3;
            fullToFree[pi * 3 + 1] = k * 3 + 1;
            fullToFree[pi * 3 + 2] = k * 3 + 2;
        }

        // Приближённые координаты для линеаризации
        double[] X = ParameterSet.fromPoints(allPoints).getInitial();

        // Сборка N только по свободным параметрам
        SimpleMatrix N = new SimpleMatrix(nFree, nFree);
        List<Measurement> meas = network.measurements();

        for (int i = 0; i < meas.size(); i++) {
            Measurement m = meas.get(i);
            double w = 1.0 / variances[i];

            double[] derivFull = new double[nAll];
            m.derivatives(X, derivFull);

            for (int rFull = 0; rFull < nAll; rFull++) {
                int r = fullToFree[rFull];
                if (r < 0) continue;
                double dr = derivFull[rFull];
                if (Math.abs(dr) < 1e-15) continue;

                for (int cFull = 0; cFull < nAll; cFull++) {
                    int c = fullToFree[cFull];
                    if (c < 0) continue;
                    double dc = derivFull[cFull];
                    if (Math.abs(dc) < 1e-15) continue;

                    N.set(r, c, N.get(r, c) + w * dr * dc);
                }
            }
        }

        // Ограничения для свободной сети — уже на сокращённой матрице
        if (network.networkType().isFree()) {
            N = applyFreeConstraints(N, network, freePointIndices);
        }

        // Обращение
        try {
            return N.invert();
        } catch (Exception e) {
            log.warn("Обычное обращение не удалось, использую SVD: {}",
                    e.getMessage());
            return svdInverse(N);
        }
    }

    /**
     * Накладывает внутренние ограничения на сокращённую N.
     * Работает с индексами определяемых точек.
     */
    private SimpleMatrix applyFreeConstraints(SimpleMatrix N,
                                              Network network,
                                              List<Integer> freeIndices) {
        var pts = network.points();
        var type = network.networkType();
        int planar = type.planarDefectRank();
        int vertical = type.verticalDefectRank();
        int defRank = planar + vertical;
        int nFree = freeIndices.size() * 3;

        if (defRank == 0) return N;

        // Центроид по определяемым точкам
        double xc = 0, yc = 0, zc = 0;
        int count = 0;
        for (int pi : freeIndices) {
            Point p = pts.get(pi);
            if (p.hasCoordinates()) {
                xc += p.x(); yc += p.y(); zc += p.z();
                count++;
            }
        }
        if (count == 0) return N;
        xc /= count; yc /= count; zc /= count;

        SimpleMatrix G = SimpleMatrix.filled(defRank, nFree, 0);
        int row = 0;

        if (planar >= 1) {
            for (int k = 0; k < freeIndices.size(); k++) {
                G.set(row, k * 3, 1.0);
            }
            row++;
        }
        if (planar >= 2) {
            for (int k = 0; k < freeIndices.size(); k++) {
                G.set(row, k * 3 + 1, 1.0);
            }
            row++;
        }
        if (planar >= 3) {
            for (int k = 0; k < freeIndices.size(); k++) {
                Point p = pts.get(freeIndices.get(k));
                double dx = (p.x() != null ? p.x() : 0) - xc;
                double dy = (p.y() != null ? p.y() : 0) - yc;
                G.set(row, k * 3, -dy);
                G.set(row, k * 3 + 1, dx);
            }
            row++;
        }
        if (vertical >= 1) {
            for (int k = 0; k < freeIndices.size(); k++) {
                G.set(row, k * 3 + 2, 1.0);
            }
            row++;
        }

        return N.plus(G.transpose().mult(G));
    }

    private SimpleMatrix svdInverse(SimpleMatrix N) {
        int n = N.numRows();
        try {
            org.apache.commons.math3.linear.RealMatrix dense =
                    new org.apache.commons.math3.linear.Array2DRowRealMatrix(n, n);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    dense.setEntry(i, j, N.get(i, j));
                }
            }
            var svd = new org.apache.commons.math3.linear.SingularValueDecomposition(dense);
            var inv = svd.getSolver().getInverse();

            SimpleMatrix Q = new SimpleMatrix(n, n);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    Q.set(i, j, inv.getEntry(i, j));
                }
            }
            return Q;
        } catch (Exception e) {
            throw new DesignException(
                    "Не удалось обратить матрицу N: " + e.getMessage(), e);
        }
    }

    /**
     * СКО координат только определяемых точек.
     */
    public Map<String, double[]> extractStdDevs(Network network,
                                                SimpleMatrix Q,
                                                double sigma0) {
        List<Point> freePoints = network.points().stream()
                .filter(p -> !p.fixed())
                .toList();

        Map<String, double[]> result = new LinkedHashMap<>();
        for (int k = 0; k < freePoints.size(); k++) {
            double[] std = new double[3];
            for (int i = 0; i < 3; i++) {
                double qii = Q.get(k * 3 + i, k * 3 + i);
                std[i] = sigma0 * Math.sqrt(Math.max(qii, 0));
            }
            result.put(freePoints.get(k).id(), std);
        }
        return result;
    }

    /**
     * Эллипсы ошибок только для определяемых точек.
     */
    public Map<String, double[]> extractErrorEllipses(Network network,
                                                      SimpleMatrix Q,
                                                      double sigma0) {
        List<Point> freePoints = network.points().stream()
                .filter(p -> !p.fixed())
                .toList();

        Map<String, double[]> result = new LinkedHashMap<>();
        for (int k = 0; k < freePoints.size(); k++) {
            double qxx = Q.get(k * 3, k * 3);
            double qyy = Q.get(k * 3 + 1, k * 3 + 1);
            double qxy = Q.get(k * 3, k * 3 + 1);

            double trace = qxx + qyy;
            double det = qxx * qyy - qxy * qxy;
            double disc = Math.sqrt(Math.max(trace * trace / 4 - det, 0));
            double l1 = trace / 2 + disc;
            double l2 = trace / 2 - disc;

            double semiMajor = sigma0 * Math.sqrt(Math.max(l1, 0));
            double semiMinor = sigma0 * Math.sqrt(Math.max(l2, 0));
            double orientation = 0.5 * Math.atan2(2 * qxy, qxx - qyy);

            result.put(freePoints.get(k).id(),
                    new double[]{semiMajor, semiMinor, orientation});
        }
        return result;
    }
}