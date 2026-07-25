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
 * Post-event fired after an entity has spawned.
 *
 * @param entity    the spawned entity
 * @param snapshot  a snapshot of the entity's state
 * @param reason    the spawn reason
 * @param cancelled whether the spawn was cancelled
 */
public record EntitySpawnPost(
    REntity entity,
    REntitySnapshot snapshot,
    String reason,
    boolean cancelled
) implements GamePostEvent {
    
    public EntitySpawnPost(REntity entity, String reason, boolean cancelled) {
        this(entity, entity.snapshot(), reason, cancelled);
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
