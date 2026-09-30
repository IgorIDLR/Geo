package ru.geodesy.adjust.design.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Service;
import ru.geodesy.adjust.design.api.PrecisionAnalysisService;
import ru.geodesy.adjust.design.api.PrecisionRequest;
import ru.geodesy.adjust.design.api.PrecisionResult;

import java.util.Map;

/**
 * Реализация прямой задачи — анализа ожидаемой точности.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrecisionAnalysisServiceImpl implements PrecisionAnalysisService {

    private final PrecisionCalculator calculator;

    @Override
    public PrecisionResult analyze(PrecisionRequest request) {
        long start = System.currentTimeMillis();

        log.info("Анализ точности: {} точек, {} измерений",
                request.network().points().size(),
                request.network().measurements().size());

        SimpleMatrix Q = calculator.computeQ(
                request.network(), request.variances());

        Map<String, double[]> stdDevs = calculator.extractStdDevs(
                request.network(), Q, request.sigma0());
        Map<String, double[]> ellipses = calculator.extractErrorEllipses(
                request.network(), Q, request.sigma0());

        double maxStd = 0;
        String worstPoint = null;
        for (var e : stdDevs.entrySet()) {
            double m = 0;
            for (double v : e.getValue()) {
                m = Math.max(m, v);
            }
            if (m > maxStd) {
                maxStd = m;
                worstPoint = e.getKey();
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Анализ завершён: max σ = {} у точки {}", maxStd, worstPoint);

        return new PrecisionResult(stdDevs, ellipses, maxStd, worstPoint, elapsed);
    }
}