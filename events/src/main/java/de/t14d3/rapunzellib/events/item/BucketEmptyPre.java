package de.t14d3.rapunzellib.events.item;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.*;
import de.t14d3.rapunzellib.registry.RItemType;

import java.util.Objects;

/**
 * Pre-event fired before a player empties a bucket (places the contents).
 *
 * <p>This event is cancellable. If denied, the bucket will not be emptied.</p>
 */
public final class BucketEmptyPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RLocation location;
    private final RItemType bucketType;


    public BucketEmptyPre(RPlayer player, RLocation location, RItemType bucketType, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.location = Objects.requireNonNull(location, "location");
        this.bucketType = Objects.requireNonNull(bucketType, "bucketType");
        setCancelled(isCancelled);
    }

    public RPlayer player() {
        return player;
    }

    /**
     * Returns the position where the bucket contents will be placed.
     *
     * @return the position
     */
    public RLocation getLocation() {
        return location;
    }

    /**
     * Returns the item type of the bucket being emptied.
     *
     * @return the bucket item type
     */
    public RItemType bucketType() {
        return bucketType;
    }
}
