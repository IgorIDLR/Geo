package ru.geodesy.adjust.deformation.internal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "geodesy.deformation")
@Data
public class DeformationProperties {

    /** Уровень значимости для теста обнаружения смещений. */
    private double significanceLevel = 0.05;

    /** Максимальное число итераций Msplit-EM. */
    private int maxIterations = 50;

    /** Точность сходимости Msplit-EM. */
    private double convergenceEps = 1e-8;

    /** Порог значимости смещения (в метрах). */
    private double significanceThreshold = 0.005;

    /** Порог вероятности разделения групп. */
    private double separationThreshold = 0.7;
}