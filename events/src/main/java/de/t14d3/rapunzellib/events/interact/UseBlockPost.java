package de.t14d3.rapunzellib.events.interact;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.block.RBlock;

/**
 * Fired after a player interacts with (right-clicks) a block.
 *
 * @param player the player who interacted
 * @param block the block that was interacted with
 * @param cancelled whether the interaction was cancelled
 */
public record UseBlockPost(RPlayer player, RBlock block, boolean cancelled) implements GamePostEvent {
}
