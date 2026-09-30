package ru.geodesy.adjust.session.internal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки модуля сессий.
 */
@Component
@ConfigurationProperties(prefix = "geodesy.session")
@Data
public class SessionProperties {

    /** Максимальное число одновременных сессий. */
    private long maxSessions = 100;

    /** TTL сессии в минутах. */
    private long ttlMinutes = 30;

    /** Порог: если патчей больше — полный пересчёт. */
    private int maxIncrementalPatches = 20;

    /** Глубина истории для отката. */
    private int historyDepth = 10;

    /** Принудительная ресинхронизация Q каждые N патчей. */
    private int resyncInterval = 100;
}