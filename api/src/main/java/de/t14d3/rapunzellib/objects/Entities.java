package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides lookup and wrapping operations for entities.
 *
 * <p>This is the central access point for resolving entities by UUID and wrapping
 * native platform entity objects.</p>
 */
public interface Entities {
    /** Looks up an entity by UUID. */
    @NotNull Optional<REntity> get(@NotNull UUID uuid);

    /** Wraps a native platform entity object into an REntity, if supported. */
    @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity);

    /** Wraps a native entity object and casts it to the requested type. */
    default <T> @NotNull Optional<T> wrap(@NotNull Object nativeEntity, @NotNull Class<T> entityType) {
        Objects.requireNonNull(nativeEntity, "nativeEntity");
        Objects.requireNonNull(entityType, "entityType");
        return wrap(nativeEntity).filter(entityType::isInstance).map(entityType::cast);
    }

    /** Looks up a player entity by UUID. */
    default @NotNull Optional<RPlayer> getPlayer(@NotNull UUID uuid) {
        return get(uuid).flatMap(REntity::asPlayer);
    }

    /** Looks up a server player by UUID. */
    default @NotNull Optional<RServerPlayer> getServerPlayer(@NotNull UUID uuid) {
        return getPlayer(uuid).flatMap(RPlayer::asServerPlayer);
    }

    /** Looks up a living entity by UUID. */
    default @NotNull Optional<RLivingEntity> getLivingEntity(@NotNull UUID uuid) {
        return get(uuid).flatMap(REntity::asLivingEntity);
    }

    /** Wraps a native entity object as a player. */
    default @NotNull Optional<RPlayer> wrapPlayer(@NotNull Object nativeEntity) {
        return wrap(nativeEntity, RPlayer.class);
    }

    /** Wraps a native entity object as a server player. */
    default @NotNull Optional<RServerPlayer> wrapServerPlayer(@NotNull Object nativeEntity) {
        return wrapPlayer(nativeEntity).flatMap(RPlayer::asServerPlayer);
    }

    /** Wraps a native entity object as a living entity. */
    default @NotNull Optional<RLivingEntity> wrapLivingEntity(@NotNull Object nativeEntity) {
        return wrap(nativeEntity, RLivingEntity.class);
    }

    /** Requires an entity by UUID, throwing if not found. */
    default @NotNull REntity require(@NotNull UUID uuid) {
        return get(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown entity: " + uuid));
    }

    /** Requires wrapping a native entity object, throwing if not possible. */
    default @NotNull REntity require(@NotNull Object nativeEntity) {
        return wrap(nativeEntity).orElseThrow(() -> new IllegalArgumentException("Cannot wrap entity: " + nativeEntity));
    }

    /** Requires wrapping a native entity into the specified type, throwing if not possible. */
    default <T> @NotNull T require(@NotNull Object nativeEntity, @NotNull Class<T> entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return wrap(nativeEntity, entityType)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap " + entityType.getSimpleName() + ": " + nativeEntity));
    }

    /** Requires a player entity by UUID, throwing if not found. */
    default @NotNull RPlayer requirePlayer(@NotNull UUID uuid) {
        return getPlayer(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown player entity: " + uuid));
    }

    /** Requires wrapping a native entity as a player, throwing if not possible. */
    default @NotNull RPlayer requirePlayer(@NotNull Object nativeEntity) {
        return wrapPlayer(nativeEntity).orElseThrow(() -> new IllegalArgumentException("Cannot wrap player entity: " + nativeEntity));
    }

    /** Requires a server player by UUID, throwing if not found. */
    default @NotNull RServerPlayer requireServerPlayer(@NotNull UUID uuid) {
        return getServerPlayer(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown server player entity: " + uuid));
    }

    /** Requires a living entity by UUID, throwing if not found. */
    default @NotNull RLivingEntity requireLivingEntity(@NotNull UUID uuid) {
        return getLivingEntity(uuid).orElseThrow(() -> new IllegalArgumentException("Unknown living entity: " + uuid));
    }

    /** Requires wrapping a native entity as a server player, throwing if not possible. */
    default @NotNull RServerPlayer requireServerPlayer(@NotNull Object nativeEntity) {
        return wrapServerPlayer(nativeEntity)
            .orElseThrow(() -> new IllegalArgumentException("Cannot wrap server player entity: " + nativeEntity));
    }

    /** Requires wrapping a native entity as a living entity, throwing if not possible. */
    default @NotNull RLivingEntity requireLivingEntity(@NotNull Object nativeEntity) {
        return wrapLivingEntity(nativeEntity).orElseThrow(() -> new IllegalArgumentException("Cannot wrap living entity: " + nativeEntity));
    }
}
