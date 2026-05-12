package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines the Worlds contract for looking up, wrapping, and enumerating worlds.
 */
public interface Worlds {

    /**
     * Returns all currently loaded worlds.
     *
     * @return a collection of all worlds
     */
    @NotNull Collection<RWorld> all();

    /**
     * Returns a world by name, if available.
     *
     * @param name the world name
     * @return an {@link Optional} containing the world, or empty if not found
     */
    @NotNull Optional<RWorld> getByName(@NotNull String name);

    /**
     * Returns a world by key, if available.
     *
     * @param key the world key
     * @return an {@link Optional} containing the world, or empty if not found
     */
    @NotNull Optional<RWorld> get(@NotNull RKey key);

    /**
     * Returns a world by string key, if available.
     *
     * @param key the world key string
     * @return an {@link Optional} containing the world, or empty if not found
     */
    default @NotNull Optional<RWorld> get(@NotNull String key) {
        return get(RKey.of(key));
    }

    /**
     * Tries to wrap a native world object into an RWorld, if supported.
     *
     * @param nativeWorld the native world object
     * @return an {@link Optional} containing the wrapped world, or empty if not supported
     */
    @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld);

    /**
     * Wraps a native world object and casts it to the requested type.
     *
     * @param nativeWorld the native world object
     * @param worldType   the expected world type class
     * @param <T>         the world type
     * @return an {@link Optional} containing the wrapped and typed world, or empty if not applicable
     */
    default <T extends RWorld> @NotNull Optional<T> wrap(@NotNull Object nativeWorld, @NotNull Class<T> worldType) {
        Objects.requireNonNull(nativeWorld, "nativeWorld");
        Objects.requireNonNull(worldType, "worldType");
        return wrap(nativeWorld).filter(worldType::isInstance).map(worldType::cast);
    }

    /**
     * Requires a world by name, throwing if not found.
     *
     * @param name the world name
     * @return the world
     * @throws IllegalArgumentException if not found
     */
    default @NotNull RWorld requireByName(@NotNull String name) {
        return getByName(name).orElseThrow(() -> new IllegalArgumentException("Unknown world: " + name));
    }

    /**
     * Requires a world by key, throwing if not found.
     *
     * @param key the world key
     * @return the world
     * @throws IllegalArgumentException if not found
     */
    default @NotNull RWorld require(@NotNull RKey key) {
        return get(key).orElseThrow(() -> new IllegalArgumentException("Unknown world: " + key));
    }

    /**
     * Requires a world by string key, throwing if not found.
     *
     * @param key the world key string
     * @return the world
     * @throws IllegalArgumentException if not found
     */
    default @NotNull RWorld require(@NotNull String key) {
        return require(RKey.of(key));
    }

    /**
     * Requires wrapping a native world object, throwing if not possible.
     *
     * @param nativeWorld the native world object
     * @return the wrapped world
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default @NotNull RWorld require(@NotNull Object nativeWorld) {
        return wrap(nativeWorld).orElseThrow(() -> new IllegalArgumentException("Cannot wrap world: " + nativeWorld));
    }

    /**
     * Requires wrapping a native world into the specified type, throwing if not possible.
     *
     * @param nativeWorld the native world object
     * @param worldType   the expected world type class
     * @param <T>         the world type
     * @return the wrapped and typed world
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default <T extends RWorld> @NotNull T require(@NotNull Object nativeWorld, @NotNull Class<T> worldType) {
        Objects.requireNonNull(worldType, "worldType");
        return wrap(nativeWorld, worldType)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap " + worldType.getSimpleName() + ": " + nativeWorld));
    }
}
