package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
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
    /**
     * Returns the immutable reference for this world.
     *
     * @return the world reference
     */
    @NotNull RWorldRef ref();

    /**
     * Returns the key for this world.
     *
     * @return the world key
     */
    default @NotNull RKey key() {
        return Objects.requireNonNull(ref().key(), "World does not expose a key");
    }

    /**
     * Returns the UUID of this world, if available.
     *
     * @return an {@link Optional} containing the world UUID, or empty if unknown
     */
    default @NotNull Optional<UUID> uuid() {
        return Optional.empty();
    }

    /**
     * Returns whether raw entity spawning semantics are available for this live world.
     */
    default boolean canSpawnEntities() {
        return false;
    }

    /**
     * Spawns a live entity using generic platform spawn semantics.
     *
     * <p>This is intentionally a thin raw action: it does not model causes or platform-specific
     * spawn options. Callers needing async-safe data should snapshot the returned entity.</p>
     */
    /**
     * Spawns an entity of the given type at the specified location.
     *
     * @param type     the registry reference for the entity type
     * @param location the spawn location
     * @return an {@link Optional} containing the spawned entity, or empty if spawning is not supported
     */
    default @NotNull Optional<REntity> spawn(@NotNull RRegistryRef<REntityType> type, @NotNull RLocation location) {
        return Optional.empty();
    }

    /**
     * Spawns an entity of the given type at the specified location.
     *
     * @param type     the entity type
     * @param location the spawn location
     * @return an {@link Optional} containing the spawned entity, or empty if spawning is not supported
     */
    default @NotNull Optional<REntity> spawn(@NotNull REntityType type, @NotNull RLocation location) {
        return spawn(REntityType.ref(type.key()), location);
    }

    /**
     * Spawns an entity of the type identified by the given key at the specified location.
     *
     * @param typeKey  the entity type key
     * @param location the spawn location
     * @return an {@link Optional} containing the spawned entity, or empty if spawning is not supported
     */
    default @NotNull Optional<REntity> spawn(@NotNull RKey typeKey, @NotNull RLocation location) {
        return spawn(REntityType.ref(typeKey), location);
    }

    /**
     * Spawns an entity of the type identified by the given string key at the specified location.
     *
     * @param typeKey  the entity type key string (e.g. {@code minecraft:zombie})
     * @param location the spawn location
     * @return an {@link Optional} containing the spawned entity, or empty if spawning is not supported
     */
    default @NotNull Optional<REntity> spawn(@NotNull String typeKey, @NotNull RLocation location) {
        return spawn(REntityType.ref(typeKey), location);
    }

    /**
     * Returns all loaded worlds.
     *
     * @return a collection of all worlds
     */
    static @NotNull Collection<RWorld> all() {
        return Rapunzel.worlds().all();
    }

    /**
     * Looks up a world by its name.
     *
     * @param name the world name
     * @return an {@link Optional} containing the world, or empty if not found
     */
    static @NotNull Optional<RWorld> getByName(@NotNull String name) {
        return Rapunzel.worlds().getByName(name);
    }

    /**
     * Looks up a world by its key.
     *
     * @param key the world key
     * @return an {@link Optional} containing the world, or empty if not found
     */
    static @NotNull Optional<RWorld> get(@NotNull RKey key) {
        return Rapunzel.worlds().get(key);
    }

    /**
     * Looks up a world by its string key.
     *
     * @param key the world key string
     * @return an {@link Optional} containing the world, or empty if not found
     */
    static @NotNull Optional<RWorld> get(@NotNull String key) {
        return Rapunzel.worlds().get(key);
    }

    /**
     * Looks up a world by its key, throwing if not found.
     *
     * @param key the world key
     * @return the world
     */
    static @NotNull RWorld require(@NotNull RKey key) {
        return Rapunzel.worlds().require(key);
    }

    /**
     * Looks up a world by its string key, throwing if not found.
     *
     * @param key the world key string
     * @return the world
     */
    static @NotNull RWorld require(@NotNull String key) {
        return Rapunzel.worlds().require(key);
    }

    /**
     * Wraps a native platform world object into an RWorld, if supported.
     *
     * @param nativeWorld the native world object
     * @return an {@link Optional} containing the wrapped world, or empty if wrapping is not supported
     */
    static @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        return Rapunzel.worlds().wrap(nativeWorld);
    }
}
