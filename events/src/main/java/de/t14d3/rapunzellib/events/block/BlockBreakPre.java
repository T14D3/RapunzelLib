package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

import java.util.Objects;

/**
 * Fired before a block is broken by a player, cancellable.
 *
 * <p>In addition to the live {@link #block()} wrapper, convenience accessors
 * {@link #blockType()} and {@link #blockTypeKey()} are provided so that callers
 * interested only in the block's type do not have to resolve it themselves.</p>
 */
public final class BlockBreakPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RBlock block;

    public BlockBreakPre(RPlayer player, RBlock block) {
        this(player, block, false);
    }

    public BlockBreakPre(RPlayer player, RBlock block, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        setCancelled(isCancelled);
    }

    public RPlayer player() {
        return player;
    }

    public RBlock block() {
        return block;
    }

    /**
     * Convenience accessor resolving the broken block's type via
     * {@code block().requireType()}. Throws if the type is not registered.
     *
     * @return the block type being broken
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
