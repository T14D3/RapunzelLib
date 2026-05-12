package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides lookup, wrapping, and enumeration operations for players.
 *
 * <p>This is the central access point for resolving players by UUID,
 * wrapping native platform player objects, and listing online players.</p>
 */
public interface Players {
    /**
     * Returns all online players across all supported platforms.
     *
     * @return a collection of online players
     */
    @NotNull Collection<RPlayer> online();

    /**
     * Looks up a player by UUID.
     *
     * @param uuid the player UUID
     * @return an {@link Optional} containing the player, or empty if not found
     */
    @NotNull Optional<RPlayer> get(@NotNull UUID uuid);

    /**
     * Wraps a native platform player object into an RPlayer, if supported.
     *
     * @param nativePlayer the native player object
     * @return an {@link Optional} containing the wrapped player, or empty if wrapping is not supported
     */
    @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer);

    /**
     * Wraps a native player object and casts it to the requested type.
     *
     * @param nativePlayer the native player object
     * @param playerType   the expected player type class
     * @param <T>          the player type
     * @return an {@link Optional} containing the wrapped and typed player, or empty if not applicable
     */
    default <T extends RPlayer> @NotNull Optional<T> wrap(@NotNull Object nativePlayer, @NotNull Class<T> playerType) {
        Objects.requireNonNull(nativePlayer, "nativePlayer");
        Objects.requireNonNull(playerType, "playerType");
        return wrap(nativePlayer).filter(playerType::isInstance).map(playerType::cast);
    }

    /**
     * Returns all online server-side players.
     *
     * @return a collection of online server players
     */
    default @NotNull Collection<RServerPlayer> onlineServers() {
        return online().stream().flatMap(player -> player.asServerPlayer().stream()).toList();
    }

    /**
     * Returns all online proxy-side players.
     *
     * @return a collection of online proxy players
     */
    default @NotNull Collection<RProxyPlayer> onlineProxies() {
        return online().stream().flatMap(player -> player.asProxyPlayer().stream()).toList();
    }

    /**
     * Looks up a server player by UUID.
     *
     * @param uuid the player UUID
     * @return an {@link Optional} containing the server player, or empty if not found
     */
    default @NotNull Optional<RServerPlayer> getServer(@NotNull UUID uuid) {
        return get(uuid).flatMap(RPlayer::asServerPlayer);
    }

    /**
     * Looks up a proxy player by UUID.
     *
     * @param uuid the player UUID
     * @return an {@link Optional} containing the proxy player, or empty if not found
     */
    default @NotNull Optional<RProxyPlayer> getProxy(@NotNull UUID uuid) {
        return get(uuid).flatMap(RPlayer::asProxyPlayer);
    }

    /**
     * Wraps a native player object as a server player.
     *
     * @param nativePlayer the native player object
     * @return an {@link Optional} containing the server player, or empty if not applicable
     */
    default @NotNull Optional<RServerPlayer> wrapServer(@NotNull Object nativePlayer) {
        return wrap(nativePlayer, RServerPlayer.class);
    }

    /**
     * Wraps a native player object as a proxy player.
     *
     * @param nativePlayer the native player object
     * @return an {@link Optional} containing the proxy player, or empty if not applicable
     */
    default @NotNull Optional<RProxyPlayer> wrapProxy(@NotNull Object nativePlayer) {
        return wrap(nativePlayer, RProxyPlayer.class);
    }

    /**
     * Requires a player by UUID, throwing if not found.
     *
     * @param uuid the player UUID
     * @return the player
     * @throws IllegalArgumentException if not found
     */
    default @NotNull RPlayer require(@NotNull UUID uuid) {
        return get(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown player: " + uuid));
    }

    /**
     * Requires wrapping a native player object, throwing if not possible.
     *
     * @param nativePlayer the native player object
     * @return the wrapped player
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default @NotNull RPlayer require(@NotNull Object nativePlayer) {
        return wrap(nativePlayer).orElseThrow(() -> new IllegalArgumentException("Cannot wrap player: " + nativePlayer));
    }

    /**
     * Requires wrapping a native player into the specified type, throwing if not possible.
     *
     * @param nativePlayer the native player object
     * @param playerType   the expected player type class
     * @param <T>          the player type
     * @return the wrapped and typed player
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default <T extends RPlayer> @NotNull T require(@NotNull Object nativePlayer, @NotNull Class<T> playerType) {
        Objects.requireNonNull(playerType, "playerType");
        return wrap(nativePlayer, playerType)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap " + playerType.getSimpleName() + ": " + nativePlayer));
    }

    /**
     * Requires a server player by UUID, throwing if not found.
     *
     * @param uuid the player UUID
     * @return the server player
     * @throws IllegalArgumentException if not found
     */
    default @NotNull RServerPlayer requireServer(@NotNull UUID uuid) {
        return getServer(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown server player: " + uuid));
    }

    /**
     * Requires a proxy player by UUID, throwing if not found.
     *
     * @param uuid the player UUID
     * @return the proxy player
     * @throws IllegalArgumentException if not found
     */
    default @NotNull RProxyPlayer requireProxy(@NotNull UUID uuid) {
        return getProxy(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown proxy player: " + uuid));
    }

    /**
     * Requires wrapping a native player as a server player, throwing if not possible.
     *
     * @param nativePlayer the native player object
     * @return the wrapped server player
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default @NotNull RServerPlayer requireServer(@NotNull Object nativePlayer) {
        return wrapServer(nativePlayer).orElseThrow(() -> new IllegalArgumentException("Cannot wrap server player: " + nativePlayer));
    }

    /**
     * Requires wrapping a native player as a proxy player, throwing if not possible.
     *
     * @param nativePlayer the native player object
     * @return the wrapped proxy player
     * @throws IllegalArgumentException if wrapping is not supported
     */
    default @NotNull RProxyPlayer requireProxy(@NotNull Object nativePlayer) {
        return wrapProxy(nativePlayer).orElseThrow(() -> new IllegalArgumentException("Cannot wrap proxy player: " + nativePlayer));
    }
}
