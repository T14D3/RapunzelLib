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

    public Optional<RLivingEntity> livingEntity() {
        return entity.asLivingEntity();
    }
}
