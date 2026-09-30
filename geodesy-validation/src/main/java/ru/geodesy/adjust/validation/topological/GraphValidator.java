package ru.geodesy.adjust.validation.topological;

import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.Point;
import ru.geodesy.adjust.model.measurement.Measurement;
import ru.geodesy.adjust.validation.ValidationError;
import ru.geodesy.adjust.validation.ValidationResult;

import java.util.*;

/**
 * Проверка связности графа сети.
 * <p>
 * Строит неориентированный граф, где вершины — точки, рёбра — измерения.
 * Затем через BFS проверяет связность. Если сеть распадается на несколько
 * компонент — сообщает, какие точки изолированы.
 */
public class GraphValidator {

    public void validate(Network network, ValidationResult result) {
        // Строим граф
        Map<String, Set<String>> graph = new HashMap<>();
        for (Point p : network.points()) {
            graph.put(p.id(), new HashSet<>());
        }
        for (Measurement m : network.measurements()) {
            String[] ids = m.involvedPointIds();
            if (ids.length >= 2) {
                for (int i = 0; i < ids.length; i++) {
                    for (int j = 0; j < ids.length; j++) {
                        if (i != j && graph.containsKey(ids[i]) && graph.containsKey(ids[j])) {
                            graph.get(ids[i]).add(ids[j]);
                        }
                    }
                }
            }
        }

        // BFS от первой точки
        if (graph.isEmpty()) return;

        String start = graph.keySet().iterator().next();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : graph.getOrDefault(current, Set.of())) {
                if (visited.add(neighbor)) {
                    queue.offer(neighbor);
                }
            }
        }

        // Проверяем, все ли точки достигнуты
        Set<String> unreached = new LinkedHashSet<>();
        for (String id : graph.keySet()) {
            if (!visited.contains(id)) {
                unreached.add(id);
            }
        }

        if (!unreached.isEmpty()) {
            result.addError(ValidationError.topology(
                    "network",
                    "Сеть не связна. Недостижимые точки: " + unreached));
        }
    }
}