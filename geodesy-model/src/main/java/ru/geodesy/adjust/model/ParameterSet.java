package ru.geodesy.adjust.model;

import ru.geodesy.common.Preconditions;

import java.util.*;

/**
 * Множество параметров уравнивания — отображение между именами
 * параметров (например, {@code "P1.x"}) и их позициями в векторе X.
 * <p>
 * Каждая точка занимает 3 последовательных параметра: X, Y, Z.
 */
public class ParameterSet {

    private final Map<String, Integer> nameToIndex;
    private final List<String> names;
    private final double[] initial;
    private final int size;

    public ParameterSet(List<String> names, double[] init) {
        Preconditions.checkState(names.size() == init.length,
                "Размеры names и init не совпадают");
        this.names = List.copyOf(names);
        this.initial = init.clone();
        this.size = names.size();

        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(names.get(i), i);
        }
        this.nameToIndex = Collections.unmodifiableMap(map);
    }

    // ============================================================
    // Фабричный метод
    // ============================================================

    /**
     * Создаёт набор параметров из списка точек.
     * Координаты отсутствующих значений инициализируются нулём.
     */
    public static ParameterSet fromPoints(List<Point> points) {
        List<String> names = new ArrayList<>(points.size() * 3);
        double[] init = new double[points.size() * 3];
        int idx = 0;
        for (Point p : points) {
            names.add(p.id() + ".x");
            init[idx++] = p.x() != null ? p.x() : 0.0;
            names.add(p.id() + ".y");
            init[idx++] = p.y() != null ? p.y() : 0.0;
            names.add(p.id() + ".z");
            init[idx++] = p.z() != null ? p.z() : 0.0;
        }
        return new ParameterSet(names, init);
    }

    // ============================================================
    // Индексация
    // ============================================================

    /**
     * Индекс параметра по имени или −1, если параметр не найден.
     */
    public int index(String name) {
        Integer i = nameToIndex.get(name);
        return i != null ? i : -1;
    }

    /**
     * Индекс координаты точки.
     *
     * @param pointId идентификатор точки
     * @param coord   0 — X, 1 — Y, 2 — Z
     * @throws IllegalStateException если точка не найдена
     */
    public int indexOf(String pointId, int coord) {
        if (coord < 0 || coord > 2) {
            throw new IllegalArgumentException("coord must be 0..2, was " + coord);
        }
        String key = pointId + (coord == 0 ? ".x" : coord == 1 ? ".y" : ".z");
        Integer i = nameToIndex.get(key);
        if (i == null) {
            throw new IllegalStateException(
                    "Точка не найдена в ParameterSet: " + pointId);
        }
        return i;
    }

    /**
     * Имя параметра по индексу.
     */
    public String name(int index) {
        Preconditions.requireInRange(index, 0, size - 1, "index");
        return names.get(index);
    }

    // ============================================================
    // Геттеры
    // ============================================================

    public int size() { return size; }

    public double[] getInitial() { return initial.clone(); }

    public List<String> names() { return names; }

    /**
     * Возвращает точку по индексу параметра или {@code null}.
     */
    public String pointIdAt(int index) {
        String full = names.get(index);
        int dot = full.lastIndexOf('.');
        return dot > 0 ? full.substring(0, dot) : null;
    }
}