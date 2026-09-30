package ru.geodesy.adjust.transformation;

import org.junit.jupiter.api.Test;
import ru.geodesy.adjust.transformation.internal.Ellipsoid;
import ru.geodesy.adjust.transformation.internal.ReductionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReductionServiceTest {

    private final ReductionService service = new ReductionService();

    // ================================================================
    // Приведение к горизонту
    // ================================================================

    @Test
    void slopeToHorizontalWithoutHeightDiffIsSame() {
        double result = service.slopeToHorizontal(100.0, 0.0);
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    void slopeToHorizontalReducesLength() {
        // S = 100 м, h = 10 м → S_horiz ≈ 100 − 100/200 = 99.5
        double result = service.slopeToHorizontal(100.0, 10.0);
        assertThat(result).isCloseTo(99.5, within(1e-6));
    }

    @Test
    void slopeToHorizontalZeroSlopeReturnsZero() {
        double result = service.slopeToHorizontal(0.0, 10.0);
        assertThat(result).isEqualTo(0.0);
    }

    // ================================================================
    // Поправка Гаусса-Крюгера
    // ================================================================

    @Test
    void gaussKrugerReductionOnCentralMeridianIsZero() {
        double result = service.gaussKrugerReduction(1000.0, 0.0, 0.0);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void gaussKrugerReductionGrowsWithDistance() {
        double r1 = service.gaussKrugerReduction(1000.0, 100_000.0, 100_000.0);
        double r2 = service.gaussKrugerReduction(1000.0, 200_000.0, 200_000.0);

        assertThat(r2).isGreaterThan(r1);
        assertThat(r1).isGreaterThan(0);
    }

    @Test
    void gaussKrugerReductionIsPositiveAndSmall() {
        // Для S = 1 км на удалении 100 км от осевого меридиана
        double result = service.gaussKrugerReduction(1000.0, 100_000.0, 100_000.0);

        // Ожидаем поправку порядка сантиметров
        assertThat(result).isGreaterThan(0);
        assertThat(result).isLessThan(1.0);
    }

    // ================================================================
    // Поправка за кривизну и рефракцию
    // ================================================================

    @Test
    void curvatureRefractionCorrectionIsPositive() {
        double result = service.curvatureRefractionCorrection(1000.0);
        assertThat(result).isGreaterThan(0);
    }

    @Test
    void curvatureRefractionCorrectionGrowsWithSquare() {
        double r1 = service.curvatureRefractionCorrection(1000.0);
        double r2 = service.curvatureRefractionCorrection(2000.0);

        // Должно расти как квадрат длины
        assertThat(r2 / r1).isCloseTo(4.0, within(0.1));
    }

    // ================================================================
    // Радиус кривизны
    // ================================================================

    @Test
    void curvatureRadiusAtEquatorIsMinimum() {
        double atEquator = service.curvatureRadius(0.0, Ellipsoid.WGS84);
        double at45 = service.curvatureRadius(Math.PI / 4, Ellipsoid.WGS84);
        double atPole = service.curvatureRadius(Math.PI / 2, Ellipsoid.WGS84);

        // Радиус меридиана минимален на экваторе, максимален на полюсе
        assertThat(atEquator).isLessThan(at45);
        assertThat(at45).isLessThan(atPole);
    }
}