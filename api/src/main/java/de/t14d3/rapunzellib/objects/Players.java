package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface Players {
    @NotNull Collection<RPlayer> online();

    @NotNull Optional<RPlayer> get(@NotNull UUID uuid);

    @NotNull Optional<RPlayer> wrap(@NotNull Object nativePlayer);

    default <T extends RPlayer> @NotNull Optional<T> wrap(@NotNull Object nativePlayer, @NotNull Class<T> playerType) {
        Objects.requireNonNull(nativePlayer, "nativePlayer");
        Objects.requireNonNull(playerType, "playerType");
        return wrap(nativePlayer).filter(playerType::isInstance).map(playerType::cast);
    }

    default @NotNull Collection<RServerPlayer> onlineServers() {
        return online().stream().flatMap(player -> player.asServerPlayer().stream()).toList();
    }

    default @NotNull Collection<RProxyPlayer> onlineProxies() {
        return online().stream().flatMap(player -> player.asProxyPlayer().stream()).toList();
    }

    default @NotNull Optional<RServerPlayer> getServer(@NotNull UUID uuid) {
        return get(uuid).flatMap(RPlayer::asServerPlayer);
    }

    default @NotNull Optional<RProxyPlayer> getProxy(@NotNull UUID uuid) {
        return get(uuid).flatMap(RPlayer::asProxyPlayer);
    }

    default @NotNull Optional<RServerPlayer> wrapServer(@NotNull Object nativePlayer) {
        return wrap(nativePlayer, RServerPlayer.class);
    }

    default @NotNull Optional<RProxyPlayer> wrapProxy(@NotNull Object nativePlayer) {
        return wrap(nativePlayer, RProxyPlayer.class);
    }

    default @NotNull RPlayer require(@NotNull UUID uuid) {
        return get(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown player: " + uuid));
    }

    default @NotNull RPlayer require(@NotNull Object nativePlayer) {
        return wrap(nativePlayer).orElseThrow(() -> new IllegalArgumentException("Cannot wrap player: " + nativePlayer));
    }

    default <T extends RPlayer> @NotNull T require(@NotNull Object nativePlayer, @NotNull Class<T> playerType) {
        Objects.requireNonNull(playerType, "playerType");
        return wrap(nativePlayer, playerType)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap " + playerType.getSimpleName() + ": " + nativePlayer));
    }

    default @NotNull RServerPlayer requireServer(@NotNull UUID uuid) {
        return getServer(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown server player: " + uuid));
    }

    default @NotNull RProxyPlayer requireProxy(@NotNull UUID uuid) {
        return getProxy(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown proxy player: " + uuid));
    }

    default @NotNull RServerPlayer requireServer(@NotNull Object nativePlayer) {
        return wrapServer(nativePlayer).orElseThrow(() -> new IllegalArgumentException("Cannot wrap server player: " + nativePlayer));
    }

    default @NotNull RProxyPlayer requireProxy(@NotNull Object nativePlayer) {
        return wrapProxy(nativePlayer).orElseThrow(() -> new IllegalArgumentException("Cannot wrap proxy player: " + nativePlayer));
    }
}
