package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Factory methods for creating entity event payloads.
 *
 * <p>Provides convenience methods for constructing post-event and snapshot
 * instances from live entity references.</p>
 */
public final class EntityEventPayloads {
    private EntityEventPayloads() {
    }

    /**
     * Creates an {@link AttackEntityPost} from player, entity, and cancelled state.
     *
     * @param player    the attacking player
     * @param entity    the attacked entity
     * @param cancelled whether the attack was cancelled
     * @return the post-event
     */
    public static @NotNull AttackEntityPost attackPost(@NotNull RPlayer player, @NotNull REntity entity, boolean cancelled) {
        return new AttackEntityPost(Objects.requireNonNull(player, "player"), Objects.requireNonNull(entity, "entity"), cancelled);
    }

    /**
     * Creates an {@link InteractEntityPost} from player, entity, and cancelled state.
     *
     * @param player    the interacting player
     * @param entity    the interacted entity
     * @param cancelled whether the interaction was cancelled
     * @return the post-event
     */
    public static @NotNull InteractEntityPost interactPost(@NotNull RPlayer player, @NotNull REntity entity, boolean cancelled) {
        return new InteractEntityPost(Objects.requireNonNull(player, "player"), Objects.requireNonNull(entity, "entity"), cancelled);
    }

    /**
     * Creates an {@link EntitySpawnPost} from entity, reason, and cancelled state.
     *
     * @param entity    the spawned entity
     * @param reason    the spawn reason
     * @param cancelled whether the spawn was cancelled
     * @return the post-event
     */
    public static @NotNull EntitySpawnPost spawnPost(@NotNull REntity entity, @NotNull String reason, boolean cancelled) {
        return new EntitySpawnPost(Objects.requireNonNull(entity, "entity"), Objects.requireNonNull(reason, "reason"), cancelled);
    }

    /**
     * Creates an {@link EntitySpawnSnapshot} from entity, reason, and cancelled state.
     *
     * @param entity    the spawned entity
     * @param reason    the spawn reason
     * @param cancelled whether the spawn was cancelled
     * @return the snapshot
     */
    public static @NotNull EntitySpawnSnapshot spawnSnapshot(@NotNull REntity entity, @NotNull String reason, boolean cancelled) {
        return EntitySpawnSnapshot.capture(Objects.requireNonNull(entity, "entity"), Objects.requireNonNull(reason, "reason"), cancelled);
    }

    /**
     * Creates an {@link EntityHurtPost} from entity, damage type, and cancelled state.
     *
     * @param entity        the hurt entity
     * @param damageTypeKey the damage type key
     * @param cancelled     whether the damage was cancelled
     * @return the post-event
     */
    public static @NotNull EntityHurtPost hurtPost(@NotNull REntity entity, @NotNull String damageTypeKey, boolean cancelled) {
        return new EntityHurtPost(Objects.requireNonNull(entity, "entity"), RKey.of(Objects.requireNonNull(damageTypeKey, "damageTypeKey")), cancelled);
    }

    /**
     * Creates an {@link EntityHurtSnapshot} from entity, damage type, and cancelled state.
     *
     * @param entity        the hurt entity
     * @param damageTypeKey the damage type key
     * @param cancelled     whether the damage was cancelled
     * @return the snapshot
     */
    public static @NotNull EntityHurtSnapshot hurtSnapshot(@NotNull REntity entity, @NotNull String damageTypeKey, boolean cancelled) {
        return EntityHurtSnapshot.capture(Objects.requireNonNull(entity, "entity"), RKey.of(Objects.requireNonNull(damageTypeKey, "damageTypeKey")), cancelled);
    }
}
