package ru.geodesy.adjust.transformation.internal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "geodesy.transformation")
@Data
public class TransformationParameters {

    private String defaultMethod = "HELMERT";
    private Map<String, SystemDefinition> systems;

    @Data
    public static class SystemDefinition {
        private String id;
        private String name;
        private DatumDefinition datum;
        private ProjectionDefinition projection;
        private double[] toWgs84;
    }

    @Data
    public static class DatumDefinition {
        private double semiMajor;
        private double flattening;
    }

    @Data
    public static class ProjectionDefinition {
        private String type;
        private double centralMeridian;
        private double falseEasting;
        private double falseNorthing;
        private double scaleFactor = 1.0;
    }
}