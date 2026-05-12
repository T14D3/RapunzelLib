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

/**
 * Abstract base implementation of {@link Worlds} that provides world lookup and caching.
 * <p>
 * Maintains a {@link ConcurrentHashMap}-backed cache of wrapped world instances keyed by {@link RKey}.
 * Implements {@link SharedWorldHooks} to serve as the bridge between the entity system and world creation.
 * </p>
 *
 * @param <W> the concrete world wrapper type managed by this implementation
 */
public abstract class SharedWorldsCore<W extends RWorld> implements Worlds, SharedWorldHooks {
    private final MinecraftServer server;
    private final ConcurrentHashMap<RKey, W> cache = new ConcurrentHashMap<>();

    /**
     * Constructs a new worlds core.
     *
     * @param server the Minecraft server instance
     */
    protected SharedWorldsCore(@NotNull MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Collection<RWorld> all() {
        Collection<RWorld> worlds = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            worlds.add(createWorld(level));
        }
        return worlds;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        Objects.requireNonNull(nativeWorld, "nativeWorld");
        return adaptNativeWorld(nativeWorld).flatMap(this::wrapNative).map(RWorld.class::cast);
    }

    /**
     * Wraps a native {@link ServerLevel} into an {@link Optional} containing the managed wrapper type.
     *
     * @param level the server level to wrap
     * @return an Optional containing the wrapped world
     */
    public final @NotNull Optional<W> wrapNative(@NotNull ServerLevel level) {
        return Optional.of(createWorld(level));
    }

    /**
     * Attempts to adapt a generic native object into a {@link ServerLevel}.
     *
     * @param nativeWorld the object to adapt
     * @return an Optional containing the adapted ServerLevel, or empty if not adaptable
     */
    protected @NotNull Optional<? extends ServerLevel> adaptNativeWorld(@NotNull Object nativeWorld) {
        return nativeWorld instanceof ServerLevel level ? Optional.of(level) : Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull W createWorld(@NotNull ServerLevel level) {
        return wrapInternal(level);
    }

    /**
     * Checks whether the given name matches the dimension location or key of the specified level.
     *
     * @param name  the name to match
     * @param level the server level to test
     * @param key   the pre-computed RKey for the level
     * @return {@code true} if the name matches
     */
    protected boolean matchesName(@NotNull String name, @NotNull ServerLevel level, @NotNull RKey key) {
        // #if VERSION >= 1.21.11
        return name.equalsIgnoreCase(level.dimension().identifier().toString()) || name.equalsIgnoreCase(key.asString());
        // #else
        return name.equalsIgnoreCase(level.dimension().location().toString()) || name.equalsIgnoreCase(key.asString());
        // #endif
    }

    /**
     * Creates a new world wrapper for the given server level.
     *
     * @param level the server level to wrap
     * @return the new wrapper instance
     */
    protected abstract @NotNull W createWorldWrapper(@NotNull ServerLevel level);

    /**
     * Updates an existing world wrapper with fresh data from the given server level.
     *
     * @param existingWorld the existing wrapper to update
     * @param level         the server level providing updated state
     */
    protected abstract void updateWorldWrapper(@NotNull W existingWorld, @NotNull ServerLevel level);

    /**
     * Internal wrapping helper that returns cached instances when available.
     *
     * @param level the server level to wrap
     * @return the cached or newly created wrapper
     */
    private @NotNull W wrapInternal(@NotNull ServerLevel level) {
        RKey key = key(level);
        return cache.compute(key, (k, existing) -> {
            if (existing == null) return createWorldWrapper(level);
            updateWorldWrapper(existing, level);
            return existing;
        });
    }

    /**
     * Extracts the {@link RKey} for the given server level.
     *
     * @param level the server level
     * @return the corresponding RKey
     */
    private static @NotNull RKey key(@NotNull ServerLevel level) {
        // #if VERSION >= 1.21.11
        return RKey.of(level.dimension().identifier().toString());
        // #else
        return RKey.of(level.dimension().location().toString());
        // #endif
    }
}
