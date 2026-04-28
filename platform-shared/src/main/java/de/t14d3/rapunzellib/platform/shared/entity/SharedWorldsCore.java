package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.Worlds;
import net.minecraft.resources.ResourceKey;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SharedWorldsCore<W extends RWorld> implements Worlds, SharedWorldHooks {
    private final MinecraftServer server;
    private final ConcurrentHashMap<RKey, W> cache = new ConcurrentHashMap<>();

    protected SharedWorldsCore(@NotNull MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public @NotNull Collection<RWorld> all() {
        Collection<RWorld> worlds = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            worlds.add(createWorld(level));
        }
        return worlds;
    }

    @Override
    public @NotNull Optional<RWorld> getByName(@NotNull String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        for (ServerLevel level : server.getAllLevels()) {
            RKey key = key(level);
            if (matchesName(name, level, key)) {
                return Optional.of(createWorld(level));
            }
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RWorld> get(@NotNull RKey key) {
        Objects.requireNonNull(key, "key");
        // #if VERSION >= 1.21.11
        Identifier location = Identifier.tryParse(key.asString());
        // #else
        ResourceLocation location = ResourceLocation.tryParse(key.asString());
        // #endif
        if (location == null) return Optional.empty();
        ServerLevel level = server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location));
        if (level == null) return Optional.empty();
        return Optional.of(createWorld(level));
    }

    @Override
    public final @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        Objects.requireNonNull(nativeWorld, "nativeWorld");
        return adaptNativeWorld(nativeWorld).flatMap(this::wrapNative).map(RWorld.class::cast);
    }

    public final @NotNull Optional<W> wrapNative(@NotNull ServerLevel level) {
        return Optional.of(createWorld(level));
    }

    protected @NotNull Optional<? extends ServerLevel> adaptNativeWorld(@NotNull Object nativeWorld) {
        return nativeWorld instanceof ServerLevel level ? Optional.of(level) : Optional.empty();
    }

    @Override
    public final @NotNull W createWorld(@NotNull ServerLevel level) {
        return wrapInternal(level);
    }

    protected boolean matchesName(@NotNull String name, @NotNull ServerLevel level, @NotNull RKey key) {
        // #if VERSION >= 1.21.11
        return name.equalsIgnoreCase(level.dimension().identifier().toString()) || name.equalsIgnoreCase(key.asString());
        // #else
        return name.equalsIgnoreCase(level.dimension().location().toString()) || name.equalsIgnoreCase(key.asString());
        // #endif
    }

    protected abstract @NotNull W createWorldWrapper(@NotNull ServerLevel level);

    protected abstract void updateWorldWrapper(@NotNull W existingWorld, @NotNull ServerLevel level);

    private @NotNull W wrapInternal(@NotNull ServerLevel level) {
        RKey key = key(level);
        return cache.compute(key, (k, existing) -> {
            if (existing == null) return createWorldWrapper(level);
            updateWorldWrapper(existing, level);
            return existing;
        });
    }

    private static @NotNull RKey key(@NotNull ServerLevel level) {
        // #if VERSION >= 1.21.11
        return RKey.of(level.dimension().identifier().toString());
        // #else
        return RKey.of(level.dimension().location().toString());
        // #endif
    }
}
