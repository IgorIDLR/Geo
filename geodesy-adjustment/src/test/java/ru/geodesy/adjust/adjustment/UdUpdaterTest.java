package ru.geodesy.adjust.adjustment;

import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.adjustment.internal.update.UdUpdater;

import static org.assertj.core.api.Assertions.assertThat;

class UdUpdaterTest {

    @Test
    void preservesSymmetryAndPositivity() {
        // Начальное Q — диагональное положительно определённое
        SimpleMatrix Q = SimpleMatrix.identity(4).scale(2.0);
        SimpleMatrix aq = new SimpleMatrix(4, 1);
        aq.set(0, 0, 1.0);
        aq.set(1, 0, -0.5);
        aq.set(2, 0, 0.3);
        aq.set(3, 0, 0.7);
        double N = 2.5;

        SimpleMatrix Qnew = UdUpdater.update(Q, aq, N);

        // Симметричность
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertThat(Qnew.get(i, j)).isCloseTo(Qnew.get(j, i),
                        org.assertj.core.data.Offset.offset(1e-12));
            }
        }

        // Положительная определённость (диагональ > 0)
        for (int i = 0; i < 4; i++) {
            assertThat(Qnew.get(i, i)).isGreaterThan(0);
        }
    }
}