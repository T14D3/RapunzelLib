package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

/**
 * Fired after a block has been broken by a player.
 *
 * <p>In addition to the {@link #block()} component, convenience accessors
 * {@link #blockType()} and {@link #blockTypeKey()} are provided so that callers
 * interested only in the block's type do not have to resolve it themselves.</p>
 *
 * @param player the player who broke the block
 * @param block the block that was broken
 * @param cancelled whether the break was cancelled
 */
public record BlockBreakPost(RPlayer player, RBlock block, boolean cancelled) implements GamePostEvent {

    /**
     * Convenience accessor resolving the broken block's type via
     * {@code block().requireType()}. Throws if the type is not registered.
     *
     * @return the block type that was broken
     */
    public RBlockType blockType() {
        return block.requireType();
    }

    /**
     * Convenience accessor returning the broken block's type key via
     * {@code block().typeKey()}.
     *
     * @return the block type key
     */
    public RKey blockTypeKey() {
        return block.typeKey();
    }
}
