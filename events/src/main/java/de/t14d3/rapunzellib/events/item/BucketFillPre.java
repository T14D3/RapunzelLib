package de.t14d3.rapunzellib.events.item;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Pre-event fired before a player fills a bucket from a fluid block.
 *
 * <p>This event is cancellable. If denied, the bucket will not be filled.</p>
 */
public final class BucketFillPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey blockTypeKey;

    /**
     * Creates a new BucketFillPre event.
     *
     * @param player      the player filling the bucket
     * @param world       the world reference
     * @param pos         the position of the fluid
     * @param blockTypeKey the block type of the fluid
     */
    public BucketFillPre(RPlayer player, RWorldRef world, RBlockPos pos, RKey blockTypeKey) {
        this(player, world, pos, blockTypeKey, false);
    }

    /**
     * Creates a new BucketFillPre event with string key.
     *
     * @param player      the player filling the bucket
     * @param world       the world reference
     * @param pos         the position of the fluid
     * @param blockTypeKey the block type of the fluid as a string
     */
    public BucketFillPre(RPlayer player, RWorldRef world, RBlockPos pos, String blockTypeKey) {
        this(player, world, pos, RKey.of(blockTypeKey));
    }

    /**
     * Creates a new BucketFillPre event with cancelled state.
     *
     * @param player      the player filling the bucket
     * @param world       the world reference
     * @param pos         the position of the fluid
     * @param blockTypeKey the block type of the fluid
     * @param isCancelled whether the event is initially cancelled
     */
    public BucketFillPre(RPlayer player, RWorldRef world, RBlockPos pos, RKey blockTypeKey, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.blockTypeKey = Objects.requireNonNull(blockTypeKey, "blockTypeKey");
        setCancelled(isCancelled);
    }

    /**
     * Creates a new BucketFillPre event with cancelled state and string key.
     *
     * @param player      the player filling the bucket
     * @param world       the world reference
     * @param pos         the position of the fluid
     * @param blockTypeKey the block type of the fluid as a string
     * @param isCancelled whether the event is initially cancelled
     */
    public BucketFillPre(RPlayer player, RWorldRef world, RBlockPos pos, String blockTypeKey, boolean isCancelled) {
        this(player, world, pos, RKey.of(blockTypeKey), isCancelled);
    }

    /**
     * Returns the player filling the bucket.
     *
     * @return the player
     */
    public RPlayer player() {
        return player;
    }

    /**
     * Returns the world where the bucket is being filled.
     *
     * @return the world
     */
    public RWorldRef world() {
        return world;
    }

    /**
     * Returns the position of the fluid.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return pos;
    }

    /**
     * Returns the block type key of the fluid.
     *
     * @return the block type key
     */
    public RKey blockTypeKey() {
        return blockTypeKey;
    }
}
