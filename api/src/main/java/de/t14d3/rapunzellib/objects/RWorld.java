package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Live server-thread world wrapper.
 */
public interface RWorld extends RNative {
    /** Returns the immutable reference for this world. */
    @NotNull RWorldRef ref();

    /** Returns the key for this world. */
    default @NotNull RKey key() {
        return Objects.requireNonNull(ref().key(), "World does not expose a key");
    }

    /** Returns the UUID of this world, if available. */
    default @NotNull Optional<UUID> uuid() {
        return Optional.empty();
    }

    /** Returns whether raw entity spawning semantics are available for this live world. */
    default boolean canSpawnEntities() {
        return false;
    }

    /** Spawns an entity of the given type at the specified location. */
    default @NotNull Optional<REntity> spawn(@NotNull RRegistryRef<REntityType> type, @NotNull RLocation location) {
        return Optional.empty();
    }

    /** Spawns an entity of the given type at the specified location. */
    default @NotNull Optional<REntity> spawn(@NotNull REntityType type, @NotNull RLocation location) {
        return spawn(REntityType.ref(type.key()), location);
    }

    /** Spawns an entity of the type identified by the given key at the specified location. */
    default @NotNull Optional<REntity> spawn(@NotNull RKey typeKey, @NotNull RLocation location) {
        return spawn(REntityType.ref(typeKey), location);
    }

    /** Spawns an entity of the type identified by the given string key at the specified location. */
    default @NotNull Optional<REntity> spawn(@NotNull String typeKey, @NotNull RLocation location) {
        return spawn(REntityType.ref(typeKey), location);
    }

    /**
     * Sets a block at the given position to the specified block data.
     *
     * @param pos  the position
     * @param data the block data to set
     * @throws UnsupportedOperationException if not supported by this implementation
     */
    default void setBlock(@NotNull RBlockPos pos, @NotNull RBlockData data) {
        RBlock.at(this, pos).setData(data);
    }

    /**
     * Fills a cuboid region between the two corners with the specified block data.
     *
     * @param min  the minimum corner (inclusive)
     * @param max  the maximum corner (inclusive)
     * @param data the block data to fill with
     * @throws UnsupportedOperationException if not supported by this implementation
     */
    default void fill(@NotNull RBlockPos min, @NotNull RBlockPos max, @NotNull RBlockData data) {
        int minX = Math.min(min.x(), max.x()), maxX = Math.max(min.x(), max.x());
        int minY = Math.min(min.y(), max.y()), maxY = Math.max(min.y(), max.y());
        int minZ = Math.min(min.z(), max.z()), maxZ = Math.max(min.z(), max.z());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    RBlock.at(this, new RBlockPos(x, y, z)).setData(data);
                }
            }
        }
    }

    /** Returns all loaded worlds. */
    static @NotNull Collection<RWorld> all() {
        return Rapunzel.worlds().all();
    }

    /** Looks up a world by its name. */
    static @NotNull Optional<RWorld> getByName(@NotNull String name) {
        return Rapunzel.worlds().getByName(name);
    }

    /** Looks up a world by its key. */
    static @NotNull Optional<RWorld> get(@NotNull RKey key) {
        return Rapunzel.worlds().get(key);
    }

    /** Looks up a world by its string key. */
    static @NotNull Optional<RWorld> get(@NotNull String key) {
        return Rapunzel.worlds().get(key);
    }

    /** Looks up a world by its key, throwing if not found. */
    static @NotNull RWorld require(@NotNull RKey key) {
        return Rapunzel.worlds().require(key);
    }

    /** Looks up a world by its string key, throwing if not found. */
    static @NotNull RWorld require(@NotNull String key) {
        return Rapunzel.worlds().require(key);
    }

    /** Wraps a native platform world object into an RWorld, if supported. */
    static @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        return Rapunzel.worlds().wrap(nativeWorld);
    }
}
