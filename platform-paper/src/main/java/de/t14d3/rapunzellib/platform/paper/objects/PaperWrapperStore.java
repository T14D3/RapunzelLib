package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.common.objects.KeyedLruCache;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.WrapperStore;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper-specific {@link WrapperStore} caching {@link RWorldRef} and
 * {@link RLocation} by their native Bukkit/NMS counterparts.
 *
 * <p>{@link RWorldRef} instances are cached by both {@link ServerLevel}
 * (the NMS handle used by {@link PaperWorlds}) and {@link World} (the
 * Bukkit handle used by event bridges), so either may be used as a lookup
 * key and the same reference is returned for equivalent world handles.</p>
 *
 * <p>{@link RLocation} instances are cached by Bukkit {@link Location} in
 * a small LRU cache, mirroring the previous behaviour of
 * {@code PaperGameEventsBridge}. Locations not affected by Minecraft
 * transformation are looked up by identity equality of the {@link Location}
 * object (since Bukkit {@link Location} implements value equality, the
 * cache will return the same {@link RLocation} for equal locations).</p>
 */
public final class PaperWrapperStore implements WrapperStore {
    /** Maximum number of cached locations (matches the former PaperGameEventsBridge size). */
    private static final int LOCATION_CACHE_SIZE = 1000;

    private final PaperWorlds worlds;

    /** Cache keyed by NMS ServerLevel, shared with {@link PaperWorlds#worldRef(ServerLevel)}. */
    private final ConcurrentHashMap<ServerLevel, RWorldRef> levelRefCache;
    /** Cache keyed by Bukkit World, used by code that only has a Bukkit handle (e.g. event bridges). */
    private final ConcurrentHashMap<World, RWorldRef> bukkitRefCache = new ConcurrentHashMap<>();
    /** LRU cache of Bukkit Location -> RLocation. */
    private final KeyedLruCache<Location, RLocation> locationCache = new KeyedLruCache<>(LOCATION_CACHE_SIZE);

    public PaperWrapperStore(@NotNull PaperWorlds worlds) {
        this.worlds = worlds;
        this.levelRefCache = worlds.worldRefCache();
    }

    @Override
    public @NotNull Optional<RWorldRef> worldRef(@NotNull Object nativeWorld) {
        if (nativeWorld instanceof ServerLevel level) {
            return Optional.of(worlds.worldRef(level));
        }
        if (nativeWorld instanceof World world) {
            return Optional.of(bukkitRefCache.computeIfAbsent(world, w ->
                new RWorldRef(w.getName(), RKey.of(w.getKey().toString()))));
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<RLocation> location(@NotNull Object nativeLocation) {
        if (nativeLocation instanceof Location loc) {
            return Optional.of(location(loc));
        }
        return Optional.empty();
    }

    /**
     * Returns the LRU cache of Bukkit {@link Location} to {@link RLocation}, exposed for
     * call sites that already hold a {@link Location} and want to bypass the
     * {@code Object} dispatch in {@link #location(Object)}.
     */
    public @NotNull KeyedLruCache<Location, RLocation> locations() {
        return locationCache;
    }

    /**
     * Returns the cached {@link RWorldRef} for a Bukkit {@link World}.
     */
    public @NotNull RWorldRef worldRef(@NotNull World world) {
        return bukkitRefCache.computeIfAbsent(world, w ->
            new RWorldRef(w.getName(), RKey.of(w.getKey().toString())));
    }

    /**
     * Returns the cached {@link RLocation} for a Bukkit {@link Location}.
     */
    public @NotNull RLocation location(@NotNull Location location) {
        return locationCache.getOrCreate(location, loc -> {
            RWorldRef ref = worldRef(loc.getWorld());
            return new RLocation(ref, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        });
    }

    /**
     * Returns the cache of Bukkit {@link World} to {@link RWorldRef}, mainly for
     * testing and direct cache inspection.
     */
    @Nullable ConcurrentHashMap<World, RWorldRef> bukkitWorldRefCache() {
        return bukkitRefCache;
    }
}
