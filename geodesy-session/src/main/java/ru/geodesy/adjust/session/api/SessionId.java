package ru.geodesy.adjust.session.api;

import java.util.UUID;

/**
 * Уникальный идентификатор сессии.
 */
public record SessionId(String value) {

    public static SessionId generate() {
        return new SessionId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}