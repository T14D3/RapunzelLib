package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RPlayer extends RAudience {
    @NotNull UUID uuid();

    @NotNull String name();

    boolean hasPermission(@NotNull String permission);

    /**
     * Returns the player's current world (server platforms only).
     */
    default @NotNull Optional<RWorld> world() {
        return Optional.empty();
    }

    /**
     * Returns the player's current location (server platforms only).
     */
    default @NotNull Optional<RLocation> location() {
        return Optional.empty();
    }

    default @NotNull RWorld worldOrThrow() {
        return world().orElseThrow(() -> new UnsupportedOperationException("world is not supported for " + getClass().getName()));
    }

    default @NotNull RLocation locationOrThrow() {
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
    default void teleport(@NotNull RLocation location) {
        throw new UnsupportedOperationException("teleport is not supported for " + getClass().getName());
    }

    /**
     * Returns the current proxy server name (proxy platforms only).
     */
    default @NotNull Optional<String> currentServerName() {
        return Optional.empty();
    }

    static @NotNull Collection<RPlayer> online() {
        return Rapunzel.players().online();
    }

    static @NotNull Optional<RPlayer> get(@NotNull UUID uuid) {
        return Rapunzel.players().get(uuid);
    }

    static @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer) {
        return Rapunzel.players().wrap(nativePlayer);
    }
}

