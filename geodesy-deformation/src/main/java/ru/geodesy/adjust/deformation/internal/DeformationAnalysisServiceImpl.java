package ru.geodesy.adjust.deformation.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.geodesy.adjust.deformation.api.*;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.parser.NetworkParser;

import java.util.*;

/**
 * Реализация сервиса анализа деформаций.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeformationAnalysisServiceImpl implements DeformationAnalysisService {

    private final NetworkParser parser;
    private final SquaredMsplitEstimator squaredMsplit;
    private final MsplitCdEstimator msplitCd;
    private final PseudoEpochEstimator pseudoEpoch;

    @Override
    public DeformationResult analyze(DeformationRequest request) {
        long start = System.currentTimeMillis();

        List<Network> networks = new ArrayList<>();
        List<String> epochIds = new ArrayList<>();
        for (EpochData epoch : request.epochs()) {
            networks.add(parser.parse(epoch.networkRawData()));
            epochIds.add(epoch.epochId());
        }

        log.info("Анализ деформаций: метод = {}, эпох = {}",
                request.method(), networks.size());

        return switch (request.method()) {
            case DeformationRequest.METHOD_SQUARED_MSPLIT ->
                    analyzeSquaredMsplit(networks, epochIds, request, start);
            case DeformationRequest.METHOD_MSPLIT_CD ->
                    analyzeMsplitCd(networks, epochIds, request, start);
            case DeformationRequest.METHOD_PSEUDO_EPOCH ->
                    analyzePseudoEpoch(networks, epochIds, request, start);
            default -> throw new DeformationException(
                    "Неизвестный метод: " + request.method());
        };
    }

    private DeformationResult analyzeSquaredMsplit(List<Network> networks,
                                                   List<String> epochIds,
                                                   DeformationRequest request,
                                                   long start) {
        Network net = networks.get(0);
        ParameterSet params = ParameterSet.fromPoints(net.points());
        double[] X = params.getInitial();
        double[] variances = net.measurements().stream()
                .mapToDouble(m -> m.getVariance()).toArray();

        var result = squaredMsplit.estimate(net, params, X, variances);
        List<PointDisplacement> disps = squaredMsplit.toDisplacements(
                result, params, net, net.sigma0());

        Map<String, List<PointDisplacement>> displacements = new LinkedHashMap<>();
        Map<String, String> classification = new LinkedHashMap<>();
        Map<String, Double> groupAssign = new LinkedHashMap<>();

        for (PointDisplacement d : disps) {
            displacements.computeIfAbsent(d.pointId(), k -> new ArrayList<>()).add(d);
            classification.put(d.pointId(), d.significant() ? "UNSTABLE" : "STABLE");
            groupAssign.put(d.pointId(), d.significant() ? 1.0 : 0.0);
        }

        return new DeformationResult(
                request.method(), epochIds, displacements, classification,
                groupAssign, 0.0, !classification.isEmpty(),
                System.currentTimeMillis() - start);
    }

    private DeformationResult analyzeMsplitCd(List<Network> networks,
                                              List<String> epochIds,
                                              DeformationRequest request,
                                              long start) {
        Network net = networks.get(0);
        ParameterSet params = ParameterSet.fromPoints(net.points());
        double[] X = params.getInitial();
        double[] variances = net.measurements().stream()
                .mapToDouble(m -> m.getVariance()).toArray();

        var result = msplitCd.estimateWithDatum(net, params, X, variances,
                request.datumDegree());
        List<PointDisplacement> disps = squaredMsplit.toDisplacements(
                result, params, net, net.sigma0());

        Map<String, List<PointDisplacement>> displacements = new LinkedHashMap<>();
        Map<String, String> classification = new LinkedHashMap<>();
        Map<String, Double> groupAssign = new LinkedHashMap<>();

        for (PointDisplacement d : disps) {
            displacements.computeIfAbsent(d.pointId(), k -> new ArrayList<>()).add(d);
            classification.put(d.pointId(), d.significant() ? "UNSTABLE" : "STABLE");
            groupAssign.put(d.pointId(), d.significant() ? 1.0 : 0.0);
        }

        return new DeformationResult(
                request.method(), epochIds, displacements, classification,
                groupAssign, 0.0, !classification.isEmpty(),
                System.currentTimeMillis() - start);
    }

    private DeformationResult analyzePseudoEpoch(List<Network> networks,
                                                 List<String> epochIds,
                                                 DeformationRequest request,
                                                 long start) {
        Network net1 = networks.get(0);
        Network net2 = networks.get(1);

        List<String> common = findCommonPoints(networks);
        Map<String, double[]> dmap = pseudoEpoch.estimate(net1, net2, common);

        Map<String, List<PointDisplacement>> displacements = new LinkedHashMap<>();
        Map<String, String> classification = new LinkedHashMap<>();
        Map<String, Double> groupAssign = new LinkedHashMap<>();

        for (var e : dmap.entrySet()) {
            double[] d = e.getValue();
            double mag = Math.sqrt(d[0]*d[0] + d[1]*d[1] + d[2]*d[2]);
            PointDisplacement pd = new PointDisplacement(
                    e.getKey(), d[0], d[1], d[2], 0, 0, 0,
                    mag, mag > 0.005, 0.95);
            displacements.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(pd);
            classification.put(e.getKey(), pd.significant() ? "UNSTABLE" : "STABLE");
            groupAssign.put(e.getKey(), pd.significant() ? 1.0 : 0.0);
        }

        return new DeformationResult(
                request.method(), epochIds, displacements, classification,
                groupAssign, 0.0, !displacements.isEmpty(),
                System.currentTimeMillis() - start);
    }

    private List<String> findCommonPoints(List<Network> networks) {
        Set<String> common = new HashSet<>();
        for (var p : networks.get(0).points()) common.add(p.id());
        for (int i = 1; i < networks.size(); i++) {
            Set<String> ids = new HashSet<>();
            for (var p : networks.get(i).points()) ids.add(p.id());
            common.retainAll(ids);
        }
        return new ArrayList<>(common);
    }
}