package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.registry.REntityType;

import java.util.Objects;

/**
 * Pre-event fired before an entity spawns in the world.
 *
 * <p>This event is cancellable. If denied, the entity will not spawn.
 * Contains the world, position, entity type, and spawn reason.</p>
 *
 * <p>The entity has not been spawned yet at the time this event fires, so a
 * live {@code REntity} wrapper is not available. The entity type is carried as
 * a typed {@link REntityType} wrapper instead.</p>
 */
public final class EntitySpawnPre extends BaseCancellablePreEvent {
    private final RLocation location;
    private final REntityType entityType;
    private final String reason;

    public EntitySpawnPre(RLocation location, REntityType entityType, String reason, boolean isCancelled) {
        this.location = Objects.requireNonNull(location, "location");
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.reason = Objects.requireNonNull(reason, "reason");
        setCancelled(isCancelled);
    }

    public RLocation location() {
        return this.location;
    }

    /**
     * Returns the typed entity type wrapper.
     *
     * @return the entity type
     */
    public REntityType entityType() {
        return entityType;
    }

    /**
     * Returns the entity type key.
     *
     * @return the entity type key
     */
    public RKey entityTypeKey() {
        return entityType.key();
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
