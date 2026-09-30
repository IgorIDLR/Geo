package ru.geodesy.adjust.session.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.geodesy.adjust.model.Network;
import ru.geodesy.adjust.model.ParameterSet;
import ru.geodesy.adjust.session.api.SessionId;

import java.util.concurrent.TimeUnit;

/**
 * Реестр сессий с LRU-вытеснением и TTL.
 */
@Slf4j
@Component
public class SessionRegistry {

    private final Cache<SessionId, AdjustmentSession> sessions;

    public SessionRegistry(SessionProperties props) {
        this.sessions = Caffeine.newBuilder()
                .maximumSize(props.getMaxSessions())
                .expireAfterAccess(props.getTtlMinutes(), TimeUnit.MINUTES)
                .removalListener((SessionId id, AdjustmentSession s, RemovalCause c) ->
                        log.info("Сессия {} удалена: {}", id, c))
                .build();
    }

    public AdjustmentSession create(SessionId id, Network network,
                                    ParameterSet params, double[] variances) {
        AdjustmentSession session = new AdjustmentSession(id, network, params, variances);
        sessions.put(id, session);
        return session;
    }

    public AdjustmentSession get(SessionId id) {
        AdjustmentSession s = sessions.getIfPresent(id);
        if (s != null) s.touch();
        return s;
    }

    public void remove(SessionId id) {
        sessions.invalidate(id);
    }
}