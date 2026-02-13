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

    default boolean isServerPlayer() {
        return this instanceof RServerPlayer;
    }

    default boolean isProxyPlayer() {
        return this instanceof RProxyPlayer;
    }

    default @NotNull Optional<RServerPlayer> asServerPlayer() {
        if (this instanceof RServerPlayer player) return Optional.of(player);
        return Optional.empty();
    }

    default @NotNull Optional<RProxyPlayer> asProxyPlayer() {
        if (this instanceof RProxyPlayer player) return Optional.of(player);
        return Optional.empty();
    }

    default boolean isEntity() {
        return this instanceof REntity;
    }

    default @NotNull Optional<REntity> asEntity() {
        if (this instanceof REntity entity) return Optional.of(entity);
        return Optional.empty();
    }

    default boolean isLivingEntity() {
        return this instanceof RLivingEntity;
    }

    default @NotNull Optional<RLivingEntity> asLivingEntity() {
        if (this instanceof RLivingEntity livingEntity) return Optional.of(livingEntity);
        return Optional.empty();
    }

    default @NotNull RServerPlayer requireServerPlayer() {
        return asServerPlayer().orElseThrow(() -> new IllegalStateException("Player does not expose server semantics: " + getClass().getName()));
    }

    default @NotNull RProxyPlayer requireProxyPlayer() {
        return asProxyPlayer().orElseThrow(() -> new IllegalStateException("Player does not expose proxy semantics: " + getClass().getName()));
    }

    default @NotNull REntity requireEntity() {
        return asEntity().orElseThrow(() -> new IllegalStateException("Player does not expose entity semantics: " + getClass().getName()));
    }

    default @NotNull RLivingEntity requireLivingEntity() {
        return asLivingEntity().orElseThrow(() -> new IllegalStateException("Player does not expose living-entity semantics: " + getClass().getName()));
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
