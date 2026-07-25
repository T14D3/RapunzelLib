package de.t14d3.rapunzellib.events.item;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

import java.util.Objects;

/**
 * Pre-event fired before a player fills a bucket from a fluid block.
 *
 * <p>This event is cancellable. If denied, the bucket will not be filled.</p>
 */
public final class BucketFillPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RBlock block;

    public BucketFillPre(RPlayer player, RBlock block) {
        this(player, block, false);
    }

    public BucketFillPre(RPlayer player, RBlock block, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        setCancelled(isCancelled);
    }

    public RPlayer player() {
        return player;
    }

    /**
     * Returns the live wrapper for the block being scooped into the bucket.
     * Use {@link #blockType()} or {@link #blockTypeKey()} for the fluid's
     * type identity and {@link #world()} / {@link #pos()} for spatial access.
     *
     * @return the live block being filled from
     */
    public RBlock block() {
        return block;
    }

    /**
     * Returns the block type of the fluid being scooped into the bucket.
     *
     * @return the fluid block type
     */
    public RBlockType blockType() {
        return block.requireType();
    }

    /**
     * Returns the key of the fluid block type.
     *
     * @return the block type key
     */
    public RKey blockTypeKey() {
        return block.typeKey();
    }

    /**
     * Returns the world reference of the fluid block.
     *
     * @return the world reference
     */
    public RWorldRef world() {
        return block.world().ref();
    }

    /**
     * Returns the position of the fluid block.
     *
     * @return the block position
     */
    public RBlockPos pos() {
        return block.pos();
    }
}
