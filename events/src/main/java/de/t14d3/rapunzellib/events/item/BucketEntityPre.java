package de.t14d3.rapunzellib.events.item;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Pre-event fired before a player uses a bucket on an entity (e.g., fish bucket).
 *
 * <p>This event is cancellable. If denied, the bucket interaction will not occur.</p>
 */
public final class BucketEntityPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey entityTypeKey;

    /**
     * Creates a new BucketEntityPre event.
     *
     * @param player        the player using the bucket
     * @param world         the world reference
     * @param pos           the position
     * @param entityTypeKey the entity type key
     */
    public BucketEntityPre(RPlayer player, RWorldRef world, RBlockPos pos, RKey entityTypeKey) {
        this(player, world, pos, entityTypeKey, false);
    }

    /**
     * Creates a new BucketEntityPre event with cancelled state.
     *
     * @param player        the player using the bucket
     * @param world         the world reference
     * @param pos           the position
     * @param entityTypeKey the entity type key
     * @param isCancelled   whether the event is initially cancelled
     */
    public BucketEntityPre(RPlayer player, RWorldRef world, RBlockPos pos, RKey entityTypeKey, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.entityTypeKey = Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        setCancelled(isCancelled);
    }

    /**
     * Returns the player using the bucket.
     *
     * @return the player
     */
    public RPlayer player() {
        return player;
    }

    /**
     * Returns the world where the bucket is being used.
     *
     * @return the world
     */
    public RWorldRef world() {
        return world;
    }

    /**
     * Returns the position.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return pos;
    }

    /**
     * Returns the entity type key.
     *
     * @return the entity type key
     */
    public RKey entityTypeKey() {
        return entityTypeKey;
    }
}
