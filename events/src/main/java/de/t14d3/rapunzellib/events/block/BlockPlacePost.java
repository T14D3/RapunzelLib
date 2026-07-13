package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

/**
 * Post-event fired after a block has been placed by a player.
 *
 * <p>Contains the player, world, position, block type, and cancellation status.</p>
 *
 * @param player      the player who placed the block
 * @param world       the world reference
 * @param pos         the position where the block was placed
 * @param blockTypeKey the block type key
 * @param cancelled   whether the placement was cancelled
 */
public record BlockPlacePost(
    RPlayer player,
    RWorldRef world,
    RBlockPos pos,
    RKey blockTypeKey,
    boolean cancelled
) implements GamePostEvent {
    
    public BlockPlacePost(RPlayer player, RWorldRef world, RBlockPos pos, String blockTypeKey, boolean cancelled) {
        this(player, world, pos, RKey.of(blockTypeKey), cancelled);
    }
}
