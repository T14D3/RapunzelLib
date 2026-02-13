package de.t14d3.rapunzellib.gui.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GuiSessionStore<S> {
    private final ConcurrentMap<UUID, S> sessions = new ConcurrentHashMap<>();

    public @Nullable S put(@NotNull UUID playerId, @NotNull S session) {
        return sessions.put(playerId, session);
    }

    public @Nullable S get(@NotNull UUID playerId) {
        return sessions.get(playerId);
    }

    public @Nullable S remove(@NotNull UUID playerId) {
        return sessions.remove(playerId);
    }

    public boolean contains(@NotNull UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public void clear() {
        sessions.clear();
    }
}
