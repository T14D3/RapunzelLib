package de.t14d3.rapunzellib.network.remote.handler;

import de.t14d3.rapunzellib.objects.RLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeferredTeleportStore {
    private static final Map<UUID, RLocation> deferred = new ConcurrentHashMap<>();

    private DeferredTeleportStore() {}

    public static void store(@NotNull UUID uuid, @NotNull RLocation location) {
        deferred.put(uuid, location);
    }

    public static @Nullable RLocation poll(@NotNull UUID uuid) {
        return deferred.remove(uuid);
    }

    public static boolean hasPending(@NotNull UUID uuid) {
        return deferred.containsKey(uuid);
    }
}
