package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
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
     * Tries to wrap a native world.
     */
    @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld);

    default @NotNull RWorld requireByName(@NotNull String name) {
        return getByName(name).orElseThrow(() -> new IllegalArgumentException("Unknown world: " + name));
    }

    default @NotNull RWorld require(@NotNull Object nativeWorld) {
        return wrap(nativeWorld).orElseThrow(() -> new IllegalArgumentException("Cannot wrap world: " + nativeWorld));
    }
}

