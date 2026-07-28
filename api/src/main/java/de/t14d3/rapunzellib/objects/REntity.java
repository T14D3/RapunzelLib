package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Live server-thread entity wrapper.
 *
 * <p>Implementations expose mutable live game state. Use {@link #snapshot()} when
 * immutable async-safe data is needed.</p>
 */
public interface REntity extends RNative {
    /** Returns the UUID of this entity. */
    @NotNull UUID uuid();

    /** Returns the registry reference for this entity's type. */
    @NotNull RRegistryRef<REntityType> typeRef();

    /** Returns the key of this entity's type. */
    default @NotNull RKey typeKey() {
        return typeRef().key();
    }

    /** Returns the world this entity is in, if available. */
    @NotNull Optional<RWorld> world();

    /** Returns the location of this entity, if available. */
    @NotNull Optional<RLocation> location();

    /** Resolves the entity type from the type reference or registry. */
    default @NotNull Optional<REntityType> type() {
        try {
            return typeRef().find();
        } catch (IllegalStateException ignored) {
            return Rapunzel.entityTypes().find(typeKey());
        }
    }

    /** Resolves the entity type, throwing if not found. */
    default @NotNull REntityType requireType() {
        try {
            return typeRef().require();
        } catch (IllegalStateException ignored) {
            return Rapunzel.entityTypes().require(typeKey());
        }
    }

    /** Returns the world reference for this entity, if available. */
    default @NotNull Optional<RWorldRef> worldRef() {
        return location().map(RLocation::world).or(() -> world().map(RWorld::ref));
    }

    /** Returns the block position of this entity, if available. */
    default @NotNull Optional<RBlockPos> blockPos() {
        return location().map(RLocation::blockPos);
    }

    /** Checks whether this entity is a player. */
    default boolean isPlayer() {
        return this instanceof RPlayer;
    }

    /** Casts this entity to a player, if applicable. */
    default @NotNull Optional<RPlayer> asPlayer() {
        if (this instanceof RPlayer player) return Optional.of(player);
        return Optional.empty();
    }

    /** Checks whether this entity is a living entity. */
    default boolean isLivingEntity() {
        return this instanceof RLivingEntity;
    }

    /** Casts this entity to a living entity, if applicable. */
    default @NotNull Optional<RLivingEntity> asLivingEntity() {
        if (this instanceof RLivingEntity livingEntity) return Optional.of(livingEntity);
        return Optional.empty();
    }

    /** Casts this entity to a living entity, throwing if not applicable. */
    default @NotNull RLivingEntity requireLivingEntity() {
        return asLivingEntity().orElseThrow(() -> new IllegalStateException("Entity does not expose living semantics: " + getClass().getName()));
    }

    /** Captures an immutable snapshot of this entity's current state. */
    default @NotNull REntitySnapshot snapshot() {
        return REntitySnapshot.capture(this);
    }

    /** Returns whether live teleport semantics are available for this wrapper. */
    default boolean canTeleport() {
        return false;
    }

    /**
     * Teleports the live entity to the provided location.
     *
     * <p>This is a raw server-thread mutation. Implementations should honor target world and
     * rotation when the backing platform supports them.</p>
     *
     * @param location the target location (world, coordinates, yaw, pitch)
     * @return true if the teleport succeeded, false otherwise
     */
    default boolean teleport(@NotNull RLocation location) {
        throw new UnsupportedOperationException("teleport is not supported for " + getClass().getName());
    }

    /** Looks up a live entity by UUID via the global entities access. */
    static @NotNull Optional<REntity> get(@NotNull UUID uuid) {
        return Rapunzel.entities().get(uuid);
    }

    /** Wraps a native platform entity object into an REntity, if supported. */
    static @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity) {
        return Rapunzel.entities().wrap(nativeEntity);
    }

    /** Wraps a native platform entity object into an REntity, throwing if not possible. */
    static @NotNull REntity of(@NotNull Object nativeEntity) {
        return Rapunzel.entities().require(nativeEntity);
    }

    /** Returns the custom name of this entity, if set. */
    @NotNull Optional<String> getName();

    /**
     * Sets the custom name of this entity.
     *
     * @param name the new custom name
     */
    void setName(@NotNull String name);

    /** Returns the display name component of this entity, if set. */
    @NotNull Optional<Component> getDisplayName();

    /**
     * Sets the display name component of this entity.
     *
     * @param displayName the new display name
     */
    void setDisplayName(@NotNull Component displayName);

    /**
     * Removes this entity from the world.
     *
     * @return true if the entity was successfully removed
     */
    boolean remove();

    /**
     * Checks whether this entity has been removed from the world.
     *
     * @return true if the entity is removed
     */
    boolean isRemoved();
}
