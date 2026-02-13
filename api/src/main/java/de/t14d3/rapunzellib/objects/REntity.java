package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
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
    @NotNull UUID uuid();

    @NotNull RRegistryRef<REntityType> typeRef();

    default @NotNull RKey typeKey() {
        return typeRef().key();
    }

    @NotNull Optional<RWorld> world();

    @NotNull Optional<RLocation> location();

    default @NotNull Optional<REntityType> type() {
        try {
            return typeRef().find();
        } catch (IllegalStateException ignored) {
            return Rapunzel.entityTypes().find(typeKey());
        }
    }

    default @NotNull REntityType requireType() {
        try {
            return typeRef().require();
        } catch (IllegalStateException ignored) {
            return Rapunzel.entityTypes().require(typeKey());
        }
    }

    default @NotNull Optional<RWorldRef> worldRef() {
        return location().map(RLocation::world).or(() -> world().map(RWorld::ref));
    }

    default @NotNull Optional<RBlockPos> blockPos() {
        return location().map(RLocation::blockPos);
    }

    default boolean isPlayer() {
        return this instanceof RPlayer;
    }

    default @NotNull Optional<RPlayer> asPlayer() {
        if (this instanceof RPlayer player) return Optional.of(player);
        return Optional.empty();
    }

    default boolean isLivingEntity() {
        return this instanceof RLivingEntity;
    }

    default @NotNull Optional<RLivingEntity> asLivingEntity() {
        if (this instanceof RLivingEntity livingEntity) return Optional.of(livingEntity);
        return Optional.empty();
    }

    default @NotNull RLivingEntity requireLivingEntity() {
        return asLivingEntity().orElseThrow(() -> new IllegalStateException("Entity does not expose living semantics: " + getClass().getName()));
    }

    default @NotNull REntitySnapshot snapshot() {
        return REntitySnapshot.capture(this);
    }

    /**
     * Returns whether live teleport semantics are available for this wrapper.
     */
    default boolean canTeleport() {
        return false;
    }

    /**
     * Teleports the live entity to the provided location.
     *
     * <p>This is a raw server-thread mutation. Implementations should honor target world and
     * rotation when the backing platform supports them.</p>
     */
    default boolean teleport(@NotNull RLocation location) {
        throw new UnsupportedOperationException("teleport is not supported for " + getClass().getName());
    }

    static @NotNull Optional<REntity> get(@NotNull UUID uuid) {
        return Rapunzel.entities().get(uuid);
    }

    static @NotNull Optional<REntity> wrap(@NotNull Object nativeEntity) {
        return Rapunzel.entities().wrap(nativeEntity);
    }
}
