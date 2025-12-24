package de.t14d3.rapunzellib.objects;

import java.util.Collection;
import java.util.Optional;

/**
 * Defines the Worlds contract (Singleton).
 */
public interface Worlds {

    /**
     * Returns all currently loaded worlds.
     */
    Collection<RWorld> all();

    /**
     * Returns a world by name, if available.
     */
    Optional<RWorld> getByName(String name);

    /**
     * Tries to wrap a native world.
     */
    Optional<RWorld> wrap(Object nativeWorld);

    default RWorld requireByName(String name) {
        return getByName(name).orElseThrow(() -> new IllegalArgumentException("Unknown world: " + name));
    }

    default RWorld require(Object nativeWorld) {
        return wrap(nativeWorld).orElseThrow(() -> new IllegalArgumentException("Cannot wrap world: " + nativeWorld));
    }
}

