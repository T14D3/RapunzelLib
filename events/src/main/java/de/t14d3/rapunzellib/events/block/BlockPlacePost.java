package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

/**
 * Post-event fired after a block has been placed by a player.
 *
 * <p>Contains the player, world, position, the live placed {@link RBlock},
 * and the cancellation status. Convenience accessors {@link #blockType()} and
 * {@link #blockTypeKey()} are provided for callers interested only in the
 * placed block's type.</p>
 *
 * @param player   the player who placed the block
 * @param block    the block that was placed
 * @param cancelled whether the placement was cancelled
 */
public record BlockPlacePost(
    RPlayer player,
    RBlock block,
    boolean cancelled
) implements GamePostEvent {

    /**
     * Convenience accessor resolving the placed block's type via
     * {@code block().requireType()}. Throws if the type is not registered.
     *
     * @return the block type that was placed
     */
    public RBlockType blockType() {
        return block.requireType();
    }

    /**
     * Convenience accessor returning the placed block's type key via
     * {@code block().typeKey()}.
     *
     * @return the block type key
     */
    public RKey blockTypeKey() {
        return block.typeKey();
    }
}
