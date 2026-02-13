package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines the Worlds contract (Singleton).
 */
public interface Worlds {

    /**
     * Returns all currently loaded worlds.
     */
    @NotNull Collection<RWorld> all();

    /**
     * Returns a world by name, if available.
     */
    @NotNull Optional<RWorld> getByName(@NotNull String name);

    /**
     * Returns a world by key, if available.
     */
    @NotNull Optional<RWorld> get(@NotNull RKey key);

    default @NotNull Optional<RWorld> get(@NotNull String key) {
        return get(RKey.of(key));
    }

    /**
     * Tries to wrap a native world.
     */
    @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld);

    default <T extends RWorld> @NotNull Optional<T> wrap(@NotNull Object nativeWorld, @NotNull Class<T> worldType) {
        Objects.requireNonNull(nativeWorld, "nativeWorld");
        Objects.requireNonNull(worldType, "worldType");
        return wrap(nativeWorld).filter(worldType::isInstance).map(worldType::cast);
    }

    default @NotNull RWorld requireByName(@NotNull String name) {
        return getByName(name).orElseThrow(() -> new IllegalArgumentException("Unknown world: " + name));
    }

    default @NotNull RWorld require(@NotNull RKey key) {
        return get(key).orElseThrow(() -> new IllegalArgumentException("Unknown world: " + key));
    }

    default @NotNull RWorld require(@NotNull String key) {
        return require(RKey.of(key));
    }

    default @NotNull RWorld require(@NotNull Object nativeWorld) {
        return wrap(nativeWorld).orElseThrow(() -> new IllegalArgumentException("Cannot wrap world: " + nativeWorld));
    }

    default <T extends RWorld> @NotNull T require(@NotNull Object nativeWorld, @NotNull Class<T> worldType) {
        Objects.requireNonNull(worldType, "worldType");
        return wrap(nativeWorld, worldType)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap " + worldType.getSimpleName() + ": " + nativeWorld));
    }
}
