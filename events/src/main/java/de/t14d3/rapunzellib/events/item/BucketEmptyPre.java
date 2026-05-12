package de.t14d3.rapunzellib.events.item;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Pre-event fired before a player empties a bucket (places the contents).
 *
 * <p>This event is cancellable. If denied, the bucket will not be emptied.</p>
 */
public final class BucketEmptyPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RWorldRef world;
    private final RBlockPos pos;
    private final String bucketTypeKey;

    /**
     * Creates a new BucketEmptyPre event.
     *
     * @param player        the player emptying the bucket
     * @param world         the world reference
     * @param pos           the position where the contents will be placed
     * @param bucketTypeKey the type of bucket being emptied
     */
    public BucketEmptyPre(RPlayer player, RWorldRef world, RBlockPos pos, String bucketTypeKey) {
        this(player, world, pos, bucketTypeKey, false);
    }

    /**
     * Creates a new BucketEmptyPre event with cancelled state.
     *
     * @param player        the player emptying the bucket
     * @param world         the world reference
     * @param pos           the position where the contents will be placed
     * @param bucketTypeKey the type of bucket being emptied
     * @param isCancelled   whether the event is initially cancelled
     */
    public BucketEmptyPre(RPlayer player, RWorldRef world, RBlockPos pos, String bucketTypeKey, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.bucketTypeKey = Objects.requireNonNull(bucketTypeKey, "bucketTypeKey");
        setCancelled(isCancelled);
    }

    /**
     * Returns the player emptying the bucket.
     *
     * @return the player
     */
    public RPlayer player() {
        return player;
    }

    /**
     * Returns the world where the bucket is being emptied.
     *
     * @return the world
     */
    public RWorldRef world() {
        return world;
    }

    /**
     * Returns the position where the bucket contents will be placed.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return pos;
    }

    /**
     * Returns the type of bucket being emptied.
     *
     * @return the bucket type key
     */
    public String bucketTypeKey() {
        return bucketTypeKey;
    }
}
