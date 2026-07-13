package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Pre-event fired before an entity spawns in the world.
 *
 * <p>This event is cancellable. If denied, the entity will not spawn.
 * Contains the world, position, entity type, and spawn reason.</p>
 */
public final class EntitySpawnPre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey entityTypeKey;
    private final String reason;

    public EntitySpawnPre(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String reason) {
        this(world, pos, entityTypeKey, reason, false);
    }

    public EntitySpawnPre(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String reason, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.entityTypeKey = Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        this.reason = Objects.requireNonNull(reason, "reason");
        setCancelled(isCancelled);
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos pos() {
        return pos;
    }

    public RKey entityTypeKey() {
        return entityTypeKey;
    }

    /**
     * Returns the spawn reason (e.g., "natural", "spawn_egg", "command").
     *
     * @return the spawn reason
     */
    public String reason() {
        return reason;
    }
}
