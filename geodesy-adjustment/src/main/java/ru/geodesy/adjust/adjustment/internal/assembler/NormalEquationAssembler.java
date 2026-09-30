package ru.geodesy.adjust.adjustment.internal.assembler;

import org.ejml.simple.SimpleMatrix;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.adjust.model.measurement.SparseMeasurement;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * Параллельная сборка нормальной матрицы N и вектора b.
 * <p>
 * Ключевые особенности:
 * <ul>
 *   <li>Отсутствие синхронизации в горячем цикле — каждый поток
 *       собирает локальный вклад, затем последовательное сведение</li>
 *   <li>Использование разреженных производных ({@link SparseMeasurement})
 *       снижает сложность с O(m·n²) до O(m·k²), где k ≈ 4–6</li>
 *   <li>Явный {@link ForkJoinPool} вместо common pool — контроль параллелизма</li>
 * </ul>
 */
public class NormalEquationAssembler {

    /** Результат сборки: нормальная матрица N и вектор b. */
    public record NormalEquations(SimpleMatrix N, SimpleMatrix b) {}

    /** Локальный вклад одного потока: (row, col, value) и b-вектор. */
    private record LocalContribution(int[] rows, int[] cols, double[] vals, double[] bLocal) {}

    private final ForkJoinPool pool;

    /**
     * Создаёт сборщик с переданным пулом.
     *
     * @param pool пул потоков (обычно бин из AppConfig)
     */
    public NormalEquationAssembler(ForkJoinPool pool) {
        this.pool = pool;
    }

    /**
     * Создаёт сборщик с отдельным пулом по числу ядер.
     * Используется в тестах и при ручном конфигурировании.
     */
    public NormalEquationAssembler() {
        this(new ForkJoinPool(Runtime.getRuntime().availableProcessors()));
    }

    /**
     * Собирает нормальные уравнения N = AᵀPA и b = AᵀP(y − f(X)).
     *
     * @param network сеть с измерениями
     * @param params  множество параметров (индексация)
     * @param X       текущий вектор параметров
     * @return N и b
     */
    public NormalEquations assemble(Network network, ParameterSet params, double[] X) {
        int n = params.size();
        List<Measurement> meas = network.measurements();
        if (meas.isEmpty()) {
            return new NormalEquations(
                    SimpleMatrix.filled(n, n, 0),
                    new SimpleMatrix(n, 1));
        }

        // Разбиваем измерения на чанки
        int parallelism = Math.max(1, pool.getParallelism());
        int chunks = Math.min(parallelism, meas.size());
        int chunkSize = (meas.size() + chunks - 1) / chunks;

        // Параллельная сборка локальных вкладов через явный pool
        List<LocalContribution> locals = pool.submit(() ->
                IntStream.range(0, chunks)
                        .parallel()
                        .mapToObj(c -> processChunk(
                                meas,
                                c * chunkSize,
                                Math.min((c + 1) * chunkSize, meas.size()),
                                params, X, n))
                        .toList()
        ).join();

        // Последовательное сведение без блокировок
        double[][] nData = new double[n][n];
        double[] bArr = new double[n];
        for (LocalContribution lc : locals) {
            for (int i = 0; i < lc.rows.length; i++) {
                nData[lc.rows[i]][lc.cols[i]] += lc.vals[i];
            }
            for (int i = 0; i < n; i++) {
                bArr[i] += lc.bLocal[i];
            }
        }

        SimpleMatrix N = new SimpleMatrix(nData);
        SimpleMatrix b = new SimpleMatrix(n, 1);
        for (int i = 0; i < n; i++) {
            b.set(i, 0, bArr[i]);
        }
        return new NormalEquations(N, b);
    }

    /**
     * Обработка одного чанка измерений: накопление вклада в локальные
     * буферы без синхронизации.
     */
    private LocalContribution processChunk(List<Measurement> meas,
                                           int start, int end,
                                           ParameterSet params,
                                           double[] X, int n) {
        // Начальная ёмкость буферов: 32 ненулевых на измерение
        int cap = Math.max(16, (end - start) * 32);
        int[] rows = new int[cap];
        int[] cols = new int[cap];
        double[] vals = new double[cap];
        int cnt = 0;

        double[] bLocal = new double[n];

        // Буферы для разреженных производных
        int[] idxBuf = new int[16];
        double[] valBuf = new double[16];

        for (int i = start; i < end; i++) {
            Measurement m = meas.get(i);
            double w = 1.0 / m.getVariance();
            double residual = m.getObserved() - m.computeValue(X);

            // Получаем производные (разреженные или полные)
            int k;
            if (m instanceof SparseMeasurement sm) {
                k = sm.sparseDerivatives(X, idxBuf, valBuf);
            } else {
                double[] full = new double[n];
                m.derivatives(X, full);
                k = 0;
                for (int j = 0; j < n; j++) {
                    if (Math.abs(full[j]) > 1e-15) {
                        idxBuf[k] = j;
                        valBuf[k] = full[j];
                        k++;
                    }
                }
            }

            // Накопление вклада по ненулевым элементам
            for (int a = 0; a < k; a++) {
                int r = idxBuf[a];
                double ar = valBuf[a];
                bLocal[r] += w * ar * residual;

                for (int c = 0; c < k; c++) {
                    // Динамическое расширение буферов
                    if (cnt == rows.length) {
                        int newCap = rows.length * 2;
                        rows = Arrays.copyOf(rows, newCap);
                        cols = Arrays.copyOf(cols, newCap);
                        vals = Arrays.copyOf(vals, newCap);
                    }
                    rows[cnt] = r;
                    cols[cnt] = idxBuf[c];
                    vals[cnt] = w * ar * valBuf[c];
                    cnt++;
                }
            }
        }

        return new LocalContribution(
                Arrays.copyOf(rows, cnt),
                Arrays.copyOf(cols, cnt),
                Arrays.copyOf(vals, cnt),
                bLocal);
    }
}