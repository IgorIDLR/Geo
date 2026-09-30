package ru.geodesy.adjust.session.internal;

import lombok.RequiredArgsConstructor;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.adjustment.api.AdjustmentRequest;
import ru.geodesy.adjust.adjustment.internal.assembler.NormalEquationAssembler;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.adjust.parser.NetworkParser;
import ru.geodesy.adjust.parser.MeasurementParser;

/**
 * Фабрика для парсинга и сборки нормальных уравнений.
 */
@Component
@RequiredArgsConstructor
public class AdjustmentEngineFactory {

    private final NetworkParser networkParser;
    private final MeasurementParser measurementParser;
    private final NormalEquationAssembler assembler;

    public Network parse(AdjustmentRequest request) {
        return networkParser.parse(request.rawData());
    }

    public double[] initialVariances(Network network, AdjustmentRequest request) {
        return network.measurements().stream()
                .mapToDouble(Measurement::getVariance)
                .toArray();
    }

    public Measurement parseSingleMeasurement(String json, AdjustmentSession session) {
        // Парсит одно измерение из JSON-строки, используя ParameterSet сессии
        // Реализация зависит от MeasurementParser
        throw new UnsupportedOperationException("Требует реализации");
    }

    public SimpleMatrix assembleFull(Network network, ParameterSet params, double[] X) {
        return assembler.assemble(network, params, X).N();
    }

    public SimpleMatrix assembleB(Network network, ParameterSet params, double[] X) {
        return assembler.assemble(network, params, X).b();
    }

    public NormalEquationAssembler.NormalEquations assembleEquations(
            Network network, ParameterSet params, double[] X) {
        return assembler.assemble(network, params, X);
    }
}