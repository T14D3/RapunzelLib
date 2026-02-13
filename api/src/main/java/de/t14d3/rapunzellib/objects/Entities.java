package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface Entities {
    @NotNull Optional<REntity> get(@NotNull UUID uuid);

    @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity);

    default <T> @NotNull Optional<T> wrap(@NotNull Object nativeEntity, @NotNull Class<T> entityType) {
        Objects.requireNonNull(nativeEntity, "nativeEntity");
        Objects.requireNonNull(entityType, "entityType");
        return wrap(nativeEntity).filter(entityType::isInstance).map(entityType::cast);
    }

    default @NotNull Optional<RPlayer> getPlayer(@NotNull UUID uuid) {
        return get(uuid).flatMap(REntity::asPlayer);
    }

    default @NotNull Optional<RServerPlayer> getServerPlayer(@NotNull UUID uuid) {
        return getPlayer(uuid).flatMap(RPlayer::asServerPlayer);
    }

    default @NotNull Optional<RLivingEntity> getLivingEntity(@NotNull UUID uuid) {
        return get(uuid).flatMap(REntity::asLivingEntity);
    }

    default @NotNull Optional<RPlayer> wrapPlayer(@NotNull Object nativeEntity) {
        return wrap(nativeEntity, RPlayer.class);
    }

    default @NotNull Optional<RServerPlayer> wrapServerPlayer(@NotNull Object nativeEntity) {
        return wrapPlayer(nativeEntity).flatMap(RPlayer::asServerPlayer);
    }

    default @NotNull Optional<RLivingEntity> wrapLivingEntity(@NotNull Object nativeEntity) {
        return wrap(nativeEntity, RLivingEntity.class);
    }

    default @NotNull REntity require(@NotNull UUID uuid) {
        return get(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown entity: " + uuid));
    }

    default @NotNull REntity require(@NotNull Object nativeEntity) {
        return wrap(nativeEntity).orElseThrow(() -> new IllegalArgumentException("Cannot wrap entity: " + nativeEntity));
    }

    default <T> @NotNull T require(@NotNull Object nativeEntity, @NotNull Class<T> entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return wrap(nativeEntity, entityType)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap " + entityType.getSimpleName() + ": " + nativeEntity));
    }

    default @NotNull RPlayer requirePlayer(@NotNull UUID uuid) {
        return getPlayer(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown player entity: " + uuid));
    }

    default @NotNull RPlayer requirePlayer(@NotNull Object nativeEntity) {
        return wrapPlayer(nativeEntity).orElseThrow(() -> new IllegalArgumentException("Cannot wrap player entity: " + nativeEntity));
    }

    default @NotNull RServerPlayer requireServerPlayer(@NotNull UUID uuid) {
        return getServerPlayer(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown server player entity: " + uuid));
    }

    default @NotNull RLivingEntity requireLivingEntity(@NotNull UUID uuid) {
        return getLivingEntity(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown living entity: " + uuid));
    }

    default @NotNull RServerPlayer requireServerPlayer(@NotNull Object nativeEntity) {
        return wrapServerPlayer(nativeEntity)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap server player entity: " + nativeEntity));
    }

    default @NotNull RLivingEntity requireLivingEntity(@NotNull Object nativeEntity) {
        return wrapLivingEntity(nativeEntity).orElseThrow(() -> new IllegalArgumentException("Cannot wrap living entity: " + nativeEntity));
    }
}
