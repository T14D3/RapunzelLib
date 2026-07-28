package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.common.objects.KeyedLruCache;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.WrapperStore;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sponge-specific {@link WrapperStore} caching {@link RWorldRef} by
 * {@link ServerWorld} and {@link RLocation} by {@link ServerLocation}.
 *
 * <p>{@link ServerLocation} instances are immutable and value-equal, so
 * caching by the location itself is sound. An LRU bound keeps memory bounded
 * under high churn (e.g. repeated entity movement).</p>
 */
public final class SpongeWrapperStore implements WrapperStore {
    private static final int LOCATION_CACHE_SIZE = 1000;

    private final ConcurrentHashMap<ServerWorld, RWorldRef> worldRefCache = new ConcurrentHashMap<>();
    private final KeyedLruCache<ServerLocation, RLocation> locationCache = new KeyedLruCache<>(LOCATION_CACHE_SIZE);

    @Override
    public @NotNull Optional<RWorldRef> worldRef(@NotNull Object nativeWorld) {
        if (nativeWorld instanceof ServerWorld world) {
            return Optional.of(worldRefCache.computeIfAbsent(world, w ->
                new RWorldRef(w.properties().name(), RKey.of(w.key().asString()))));
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RLocation> location(@NotNull Object nativeLocation) {
        if (nativeLocation instanceof ServerLocation loc) {
            return Optional.of(location(loc));
        }
        return Optional.empty();
    }

    @Override
    public @NotNull String toString() {
        return "SpongeWrapperStore";
    }

    /**
     * Returns the cached {@link RWorldRef} for a {@link ServerWorld}, bypassing
     * the {@code Object} dispatch in {@link #worldRef(Object)}.
     */
    public @NotNull RWorldRef worldRef(@NotNull ServerWorld world) {
        Objects.requireNonNull(world, "world");
        return worldRefCache.computeIfAbsent(world, w ->
            new RWorldRef(w.properties().name(), RKey.of(w.key().asString())));
    }

    /**
     * Returns the cached {@link RLocation} for a {@link ServerLocation}.
     */
    public @NotNull RLocation location(@NotNull ServerLocation location) {
        Objects.requireNonNull(location, "location");
        return locationCache.getOrCreate(location, l -> {
            RWorldRef ref = worldRef(l.world());
            return new RLocation(ref, l.x(), l.y(), l.z(), 0f, 0f);
        });
    }
}
