package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventSnapshot;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;

import java.util.UUID;

/**
 * Immutable snapshot of an entity spawn event for async processing.
 *
 * @param entity    a snapshot of the entity's state
 * @param reason    the spawn reason
 * @param cancelled whether the spawn was cancelled
 */
public record EntitySpawnSnapshot(
 REntitySnapshot entity,
 String reason,
 boolean cancelled
) implements GameEventSnapshot {
    /**
     * Creates a snapshot from raw world/position/entity type data.
     *
     * @param world         the world reference
     * @param pos           the block position
     * @param entityTypeKey the entity type key
     * @param reason        the spawn reason
     * @param cancelled     whether the spawn was cancelled
     */
    public EntitySpawnSnapshot(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String reason, boolean cancelled) {
        this(REntitySnapshot.of(UUID.randomUUID(), world, pos, entityTypeKey), reason, cancelled);
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
     * @param entity    the live entity
     * @param reason    the spawn reason
     * @param cancelled whether the spawn was cancelled
     * @return the captured snapshot
     */
    public static EntitySpawnSnapshot capture(REntity entity, String reason, boolean cancelled) {
        return new EntitySpawnSnapshot(entity.snapshot(), reason, cancelled);
    }
}
