package de.t14d3.rapunzellib.gui.shared.map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of open map sessions, keyed by player.
 * <p>
 * Exactly one map session can be open per player; opening a second map for
 * the same player replaces the first.
 * </p>
 */
public final class SharedMapSessions {

    private static final Map<UUID, SharedMapSession> SESSIONS = new ConcurrentHashMap<>();

    private SharedMapSessions() {
    }

    public static void register(@NotNull UUID playerId, @NotNull SharedMapSession session) {
        SESSIONS.put(playerId, session);
    }

    @Nullable
    public static SharedMapSession get(@NotNull UUID playerId) {
        return SESSIONS.get(playerId);
    }

    /** Closes and removes any session for the player. */
    public static void close(@NotNull UUID playerId) {
        SharedMapSession session = SESSIONS.remove(playerId);
        if (session != null) {
            session.close();
        }
    }
}
