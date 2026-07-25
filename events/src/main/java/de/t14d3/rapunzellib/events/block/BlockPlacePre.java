package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

import java.util.Objects;

/**
 * Pre-event fired before a block is placed by a player.
 *
 * <p>This event is cancellable. If denied, the block will not be placed.
 * Contains information about the player, world, position, and the live
 * {@link RBlock} that is being placed.</p>
 *
 * <p>Convenience accessors {@link #blockType()} and {@link #blockTypeKey()}
 * are provided so that callers interested only in the placed block's type do
 * not have to resolve it themselves. The {@code world} and {@code pos} fields
 * are retained alongside the live {@code RBlock} because the bridge knows them
 * independently and callers may rely on them directly.</p>
 */
public final class BlockPlacePre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RBlock block;


    public BlockPlacePre(RPlayer player, RBlock block, boolean isCancelled) {
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
     * Convenience accessor resolving the placed block's type via
     * {@code block().requireType()}. Throws if the type is not registered.
     *
     * @return the block type being placed
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
