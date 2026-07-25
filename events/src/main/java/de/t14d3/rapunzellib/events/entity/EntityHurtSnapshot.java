package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventSnapshot;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;

import java.util.UUID;

/**
 * Immutable snapshot of an entity hurt event for async processing.
 *
 * @param entity        a snapshot of the entity's state
 * @param damageTypeKey the damage type key
 * @param cancelled     whether the damage was cancelled
 */
public record EntityHurtSnapshot(
 REntitySnapshot entity,
 RKey damageTypeKey,
 boolean cancelled
) implements GameEventSnapshot {
    /**
     * Creates a snapshot from raw world/position/entity type data.
     *
     * @param world         the world reference
     * @param pos           the block position
     * @param entityTypeKey the entity type key
     * @param damageTypeKey the damage type key
     * @param cancelled     whether the damage was cancelled
     */
    public EntityHurtSnapshot(RWorldRef world, RBlockPos pos, RKey entityTypeKey, RKey damageTypeKey, boolean cancelled) {
        this(REntitySnapshot.of(UUID.randomUUID(), world, pos, entityTypeKey), damageTypeKey, cancelled);
    }

    /**
     * Creates a snapshot from raw world/position/entity type data with string
     * damage type key.
     *
     * @param world         the world reference
     * @param pos           the block position
     * @param entityTypeKey the entity type key
     * @param damageTypeKey the damage type key string
     * @param cancelled     whether the damage was cancelled
     */
    public EntityHurtSnapshot(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String damageTypeKey, boolean cancelled) {
        this(REntitySnapshot.of(UUID.randomUUID(), world, pos, entityTypeKey), RKey.of(damageTypeKey), cancelled);
    }

    public RWorldRef world() {
        return entity.world();
    }

    public RBlockPos pos() {
        return entity.pos();
    }

    public RKey entityTypeKey() {
        return entity.entityTypeKey();
    }

    /**
     * Captures a snapshot from a live entity reference.
     *
     * @param entity        the live entity
     * @param damageTypeKey the damage type key
     * @param cancelled     whether the damage was cancelled
     * @return the captured snapshot
     */
    public static EntityHurtSnapshot capture(REntity entity, RKey damageTypeKey, boolean cancelled) {
        return new EntityHurtSnapshot(entity.snapshot(), damageTypeKey, cancelled);
    }

    /**
     * Captures a snapshot from a live entity reference with a string damage
     * type key.
     *
     * @param entity        the live entity
     * @param damageTypeKey the damage type key string
     * @param cancelled     whether the damage was cancelled
     * @return the captured snapshot
     */
    public static EntityHurtSnapshot capture(REntity entity, String damageTypeKey, boolean cancelled) {
        return new EntityHurtSnapshot(entity.snapshot(), RKey.of(damageTypeKey), cancelled);
    }
}
