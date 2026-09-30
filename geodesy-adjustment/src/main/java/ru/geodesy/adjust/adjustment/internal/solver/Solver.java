package ru.geodesy.adjust.adjustment.internal.solver;

import org.ejml.simple.SimpleMatrix;

public interface Solver {
    SimpleMatrix solve(SimpleMatrix N, SimpleMatrix b);
    SimpleMatrix invert(SimpleMatrix N);
}