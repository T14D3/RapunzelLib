package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RPlayer extends RAudience {
    UUID uuid();

    String name();

    boolean hasPermission(String permission);

    /**
     * Returns the player's current world (server platforms only).
     */
    default Optional<RWorld> world() {
        return Optional.empty();
    }

    /**
     * Returns the player's current location (server platforms only).
     */
    default Optional<RLocation> location() {
        return Optional.empty();
    }

    default RWorld worldOrThrow() {
        return world().orElseThrow(() -> new UnsupportedOperationException("world is not supported for " + getClass().getName()));
    }

    default RLocation locationOrThrow() {
        return location().orElseThrow(() -> new UnsupportedOperationException("location is not supported for " + getClass().getName()));
    }

    default boolean canTeleport() {
        return false;
    }

    /**
     * Teleports the player (server platforms only).
     *
     * @throws UnsupportedOperationException if teleporting is not supported by this platform/player implementation
     */
    default void teleport(RLocation location) {
        throw new UnsupportedOperationException("teleport is not supported for " + getClass().getName());
    }

    /**
     * Returns the current proxy server name (proxy platforms only).
     */
    default Optional<String> currentServerName() {
        return Optional.empty();
    }

    static Collection<RPlayer> online() {
        return Rapunzel.players().online();
    }

    static Optional<RPlayer> get(UUID uuid) {
        return Rapunzel.players().get(uuid);
    }

    static Optional<RPlayer> wrap(Object nativePlayer) {
        return Rapunzel.players().wrap(nativePlayer);
    }
}

