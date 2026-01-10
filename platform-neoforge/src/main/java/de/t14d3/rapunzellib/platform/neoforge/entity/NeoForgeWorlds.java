package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoForgeWorlds implements Worlds {
    private final MinecraftServer server;
    private final ConcurrentHashMap<String, NeoForgeWorld> cache = new ConcurrentHashMap<>();

    public NeoForgeWorlds(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public @NotNull Collection<RWorld> all() {
        Collection<RWorld> worlds = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            worlds.add(wrapInternal(level));
        }
        return worlds;
    }

    @Override
    public @NotNull Optional<RWorld> getByName(@NotNull String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        for (ServerLevel level : server.getAllLevels()) {
            String key = level.dimension().location().toString();
            if (name.equalsIgnoreCase(key)) {
                return Optional.of(wrapInternal(level));
            }
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        if (!(nativeWorld instanceof ServerLevel level)) return Optional.empty();
        return Optional.of(wrapInternal(level));
    }

    private NeoForgeWorld wrapInternal(ServerLevel level) {
        String key = level.dimension().location().toString();
        return cache.compute(key, (k, existing) -> {
            if (existing == null) return new NeoForgeWorld(level);
            existing.updateHandle(level);
            return existing;
        });
    }
}
