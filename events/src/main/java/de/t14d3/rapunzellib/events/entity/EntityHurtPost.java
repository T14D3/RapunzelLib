package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;

import java.util.Optional;

/**
 * Post-event fired after an entity has taken damage.
 *
 * @param entity        the entity that was hurt
 * @param snapshot      a snapshot of the entity's state
 * @param damageTypeKey the damage type key
 * @param cancelled     whether the damage was cancelled
 */
public record EntityHurtPost(
    REntity entity,
    REntitySnapshot snapshot,
    String damageTypeKey,
    boolean cancelled
) implements GamePostEvent {
    /**
     * Creates an EntityHurtPost from an entity, damage type, and cancelled state.
     *
     * @param entity        the entity that was hurt
     * @param damageTypeKey the damage type key
     * @param cancelled     whether the damage was cancelled
     */
    public EntityHurtPost(REntity entity, String damageTypeKey, boolean cancelled) {
        this(entity, entity.snapshot(), damageTypeKey, cancelled);
    }

    /**
     * Returns the world from the snapshot.
     *
     * @return the world
     */
    public RWorldRef world() {
        return snapshot.world();
    }

    /**
     * Returns the position from the snapshot.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return snapshot.pos();
    }

    /**
     * Returns the entity type key from the snapshot.
     *
     * @return the entity type key
     */
    public RKey entityTypeKey() {
        return snapshot.entityTypeKey();
    }

    /**
     * Returns the entity as a living entity, if applicable.
     *
     * @return an optional containing the living entity
     */
    public Optional<RLivingEntity> livingEntity() {
        return entity.asLivingEntity();
    }
}
