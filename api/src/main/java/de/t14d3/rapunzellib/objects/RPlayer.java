package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.RCommandDispatcher;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a connected player, either on a game server or a proxy.
 *
 * <p>This is the base player interface. Depending on the platform, a player
 * may also implement {@link RServerPlayer}, {@link RProxyPlayer}, or both.</p>
 */
public interface RPlayer extends RAudience, RCommandDispatcher {
    /** Returns the UUID of this player. */
    @NotNull UUID uuid();

    /** Returns the username of this player. */
    @NotNull String name();

    /**
     * Checks whether this player has the given permission.
     *
     * @param permission the permission node to check
     * @return true if the player has the permission
     */
    boolean hasPermission(@NotNull String permission);

    /** Checks whether this player is a server-side player. */
    default boolean isServerPlayer() {
        return this instanceof RServerPlayer;
    }

    /** Checks whether this player is a proxy-side player. */
    default boolean isProxyPlayer() {
        return this instanceof RProxyPlayer;
    }

    /** Casts this player to a server player, if applicable. */
    default @NotNull Optional<RServerPlayer> asServerPlayer() {
        if (this instanceof RServerPlayer player) return Optional.of(player);
        return Optional.empty();
    }

    /** Casts this player to a proxy player, if applicable. */
    default @NotNull Optional<RProxyPlayer> asProxyPlayer() {
        if (this instanceof RProxyPlayer player) return Optional.of(player);
        return Optional.empty();
    }

    /** Checks whether this player is also an entity. */
    default boolean isEntity() {
        return this instanceof REntity;
    }

    /** Casts this player to an entity, if applicable. */
    default @NotNull Optional<REntity> asEntity() {
        if (this instanceof REntity entity) return Optional.of(entity);
        return Optional.empty();
    }

    /** Checks whether this player is also a living entity. */
    default boolean isLivingEntity() {
        return this instanceof RLivingEntity;
    }

    /** Casts this player to a living entity, if applicable. */
    default @NotNull Optional<RLivingEntity> asLivingEntity() {
        if (this instanceof RLivingEntity livingEntity) return Optional.of(livingEntity);
        return Optional.empty();
    }

    /** Casts this player to a server player, throwing if not applicable. */
    default @NotNull RServerPlayer requireServerPlayer() {
        return asServerPlayer().orElseThrow(() -> new IllegalStateException("Player does not expose server semantics: " + getClass().getName()));
    }

    /** Casts this player to a proxy player, throwing if not applicable. */
    default @NotNull RProxyPlayer requireProxyPlayer() {
        return asProxyPlayer().orElseThrow(() -> new IllegalStateException("Player does not expose proxy semantics: " + getClass().getName()));
    }

    /** Casts this player to an entity, throwing if not applicable. */
    default @NotNull REntity requireEntity() {
        return asEntity().orElseThrow(() -> new IllegalStateException("Player does not expose entity semantics: " + getClass().getName()));
    }

    /** Casts this player to a living entity, throwing if not applicable. */
    default @NotNull RLivingEntity requireLivingEntity() {
        return asLivingEntity().orElseThrow(() -> new IllegalStateException("Player does not expose living-entity semantics: " + getClass().getName()));
    }

    default @NotNull Optional<RLocation> location() {
        return this instanceof RServerPlayer serverPlayer ? serverPlayer.location() : Optional.empty();
    }

    /** Returns all currently online players. */
    static @NotNull Collection<RPlayer> online() {
        return Rapunzel.players().online();
    }

    /** Looks up a player by UUID via the global players access. */
    static @NotNull Optional<RPlayer> get(@NotNull UUID uuid) {
        return Rapunzel.players().get(uuid);
    }

    /** Wraps a native platform player object into an RPlayer, if supported. */
    static @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer) {
        return Rapunzel.players().wrap(nativePlayer);
    }

    /** Wraps a native platform player object into an RPlayer, throwing if not possible. */
    static @NotNull RPlayer of(@NotNull Object nativePlayer) {
        return Rapunzel.players().require(nativePlayer);
    }

    @Override
    default void dispatch(@NotNull String command) {
        // Platform implementations should override with proper command-packet dispatch.
        String msg = command.startsWith("/") ? command : "/" + command;
        sendMessage(Component.text(msg));
    }
}
