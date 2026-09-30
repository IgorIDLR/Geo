package ru.geodesy.adjust.transformation.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.transformation.api.*;
import ru.geodesy.common.ValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Фабрика трансформеров. Автоматически собирает все бины,
 * реализующие {@link CoordinateTransformer}.
 */
@Slf4j
@Component
public class TransformationFactory {

    private final Map<TransformationMethod, CoordinateTransformer> transformers
            = new HashMap<>();
    private final TransformationParameters parameters;

    public TransformationFactory(ApplicationContext ctx,
                                 TransformationParameters parameters) {
        this.parameters = parameters;
        var beans = ctx.getBeansOfType(CoordinateTransformer.class);
        for (CoordinateTransformer t : beans.values()) {
            transformers.put(t.method(), t);
            log.debug("Зарегистрирован трансформер: {}", t.method());
        }
    }

    public CoordinateTransformer create(TransformationMethod method) {
        CoordinateTransformer t = transformers.get(method);
        if (t == null) {
            throw new ValidationException(
                    "Метод трансформации не реализован: " + method
                            + " (доступны: " + transformers.keySet() + ")");
        }
        return t;
    }

    public CoordinateSystem getSystem(String id) {
        if (parameters.getSystems() == null
                || !parameters.getSystems().containsKey(id)) {
            throw new ValidationException("Система координат не найдена: " + id);
        }
        TransformationParameters.SystemDefinition def = parameters.getSystems().get(id);

        Ellipsoid ellipsoid = new Ellipsoid(
                def.getName() != null ? def.getName() : id,
                def.getDatum().getSemiMajor(),
                def.getDatum().getFlattening());

        CoordinateSystem.Projection projection = null;
        if (def.getProjection() != null && def.getProjection().getType() != null) {
            try {
                projection = new CoordinateSystem.Projection(
                        CoordinateSystem.Projection.ProjectionType.valueOf(
                                def.getProjection().getType().toUpperCase()),
                        def.getProjection().getCentralMeridian(),
                        def.getProjection().getFalseEasting(),
                        def.getProjection().getFalseNorthing(),
                        def.getProjection().getScaleFactor());
            } catch (IllegalArgumentException e) {
                throw new ValidationException(
                        "Неизвестный тип проекции: " + def.getProjection().getType(), e);
            }
        }

        return new CoordinateSystem(
                def.getId() != null ? def.getId() : id,
                def.getName(),
                ellipsoid,
                projection,
                def.getToWgs84());
    }
}