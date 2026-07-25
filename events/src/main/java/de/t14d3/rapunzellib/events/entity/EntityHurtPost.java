package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import de.t14d3.rapunzellib.registry.REntityType;

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
    RKey damageTypeKey,
    boolean cancelled
) implements GamePostEvent {
    /**
     * Creates an EntityHurtPost from an entity, damage type, and cancelled state.
     *
     * @param entity        the entity that was hurt
     * @param damageTypeKey the damage type key
     * @param cancelled     whether the damage was cancelled
     */
    public EntityHurtPost(REntity entity, RKey damageTypeKey, boolean cancelled) {
        this(entity, entity.snapshot(), damageTypeKey, cancelled);
    }

    /**
     * Creates an EntityHurtPost from an entity, damage type string, and cancelled state.
     *
     * @param entity        the entity that was hurt
     * @param damageTypeKey the damage type key string
     * @param cancelled     whether the damage was cancelled
     */
    public EntityHurtPost(REntity entity, String damageTypeKey, boolean cancelled) {
        this(entity, entity.snapshot(), RKey.of(damageTypeKey), cancelled);
    }

    public RWorldRef world() {
        return snapshot.world();
    }

    public RBlockPos pos() {
        return snapshot.pos();
    }

    public RKey entityTypeKey() {
        return snapshot.entityTypeKey();
    }

    /**
     * Returns the typed entity type wrapper, resolved from the live entity.
     *
     * @return the entity type
     */
    public REntityType entityType() {
        return entity.requireType();
    }

    public Optional<RLivingEntity> livingEntity() {
        return entity.asLivingEntity();
    }
}
