package de.t14d3.rapunzellib.events.item;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.*;
import de.t14d3.rapunzellib.registry.REntityType;

import java.util.Objects;

/**
 * Pre-event fired before a player uses a bucket on an entity (e.g., fish bucket).
 *
 * <p>This event is cancellable. If denied, the bucket interaction will not occur.</p>
 */
public final class BucketEntityPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RLocation location;
    private final REntity entity;


    public BucketEntityPre(RPlayer player, RLocation location, REntity entity, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.location = Objects.requireNonNull(location, "location");
        this.entity = Objects.requireNonNull(entity, "entity");
        setCancelled(isCancelled);
    }

    public RPlayer player() {
        return player;
    }

    public RLocation getLocation() {
        return location;
    }

    /**
     * Returns the live wrapper for the entity being captured into the bucket.
     *
     * @return the live entity
     */
    public REntity entity() {
        return entity;
    }

    /**
     * Returns the entity type of the captured entity.
     *
     * @return the entity type
     */
    public REntityType entityType() {
        return entity.requireType();
    }

    /**
     * Returns the key of the captured entity's type.
     *
     * @return the entity type key
     */
    public RKey entityTypeKey() {
        return entity.typeKey();
    }
}
