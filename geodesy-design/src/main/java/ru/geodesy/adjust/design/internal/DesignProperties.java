package ru.geodesy.adjust.design.internal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки модуля проектирования.
 */
@Component
@ConfigurationProperties(prefix = "geodesy.design")
@Data
public class DesignProperties {

    private int maxWeightIterations = 20;
    private double convergenceEps = 1e-4;
    private double maxWeightFactor = 100.0;
    private double minWeightFactor = 0.01;

    private int geneticPopulationSize = 100;
    private int geneticGenerations = 50;
    private double geneticMutationRate = 0.1;
}