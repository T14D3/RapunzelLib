package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.block.RBlock;

import java.util.Objects;

/**
 * Fired before a block is broken by a player, cancellable.
 */
public final class BlockBreakPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RBlock block;

    /**
     * Creates a new non-cancelled block break pre-event.
     *
     * @param player the player breaking the block
     * @param block the block being broken
     */
    public BlockBreakPre(RPlayer player, RBlock block) {
        this(player, block, false);
    }

    /**
     * Creates a new block break pre-event.
     *
     * @param player the player breaking the block
     * @param block the block being broken
     * @param isCancelled initial cancellation state
     */
    public BlockBreakPre(RPlayer player, RBlock block, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        setCancelled(isCancelled);
    }

    /**
     * Returns the player breaking the block.
     *
     * @return the player
     */
    public RPlayer player() {
        return player;
    }

    /**
     * Returns the block being broken.
     *
     * @return the block
     */
    public RBlock block() {
        return block;
    }
}

