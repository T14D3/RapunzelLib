package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.block.RBlock;

/**
 * Fired after a block has been broken by a player.
 *
 * @param player the player who broke the block
 * @param block the block that was broken
 * @param cancelled whether the break was cancelled
 */
public record BlockBreakPost(RPlayer player, RBlock block, boolean cancelled) implements GamePostEvent {
}
