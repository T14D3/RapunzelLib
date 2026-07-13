package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines the Worlds contract for looking up, wrapping, and enumerating worlds.
 */
public interface Worlds {

    /** Returns all currently loaded worlds. */
    @NotNull Collection<RWorld> all();

    /** Returns a world by name, if available. */
    @NotNull Optional<RWorld> getByName(@NotNull String name);

    /** Returns a world by key, if available. */
    @NotNull Optional<RWorld> get(@NotNull RKey key);

    /** Returns a world by string key, if available. */
    default @NotNull Optional<RWorld> get(@NotNull String key) {
        return get(RKey.of(key));
    }

    /** Tries to wrap a native world object into an RWorld, if supported. */
    @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld);

    /** Wraps a native world object and casts it to the requested type. */
    default <T extends RWorld> @NotNull Optional<T> wrap(@NotNull Object nativeWorld, @NotNull Class<T> worldType) {
        Objects.requireNonNull(nativeWorld, "nativeWorld");
        Objects.requireNonNull(worldType, "worldType");
        return wrap(nativeWorld).filter(worldType::isInstance).map(worldType::cast);
    }

    /** Requires a world by name, throwing if not found. */
    default @NotNull RWorld requireByName(@NotNull String name) {
        return getByName(name).orElseThrow(() -> new IllegalArgumentException("Unknown world: " + name));
    }

    /** Requires a world by key, throwing if not found. */
    default @NotNull RWorld require(@NotNull RKey key) {
        return get(key).orElseThrow(() -> new IllegalArgumentException("Unknown world: " + key));
    }

    /** Requires a world by string key, throwing if not found. */
    default @NotNull RWorld require(@NotNull String key) {
        return require(RKey.of(key));
    }

    /** Requires wrapping a native world object, throwing if not possible. */
    default @NotNull RWorld require(@NotNull Object nativeWorld) {
        return wrap(nativeWorld).orElseThrow(() -> new IllegalArgumentException("Cannot wrap world: " + nativeWorld));
    }

    /** Requires wrapping a native world into the specified type, throwing if not possible. */
    default <T extends RWorld> @NotNull T require(@NotNull Object nativeWorld, @NotNull Class<T> worldType) {
        Objects.requireNonNull(worldType, "worldType");
        return wrap(nativeWorld, worldType)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap " + worldType.getSimpleName() + ": " + nativeWorld));
    }
}
