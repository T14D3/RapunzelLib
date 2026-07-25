package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RItemType;

import java.util.Objects;

/**
 * Dispatched before a block is destroyed by a non-player world event
 * (e.g., {@code /setblock ... destroy}, lava consuming blocks, etc.).
 *
 * <p>This is distinct from {@link BlockBreakPre} (player-initiated breaking)
 * and {@link BlockTransformPre} (block-to-block transformation).  The
 * {@code DESTROY} flag in Zones maps to this event.</p>
 *
 * <p>The destroyed block is supplied as a live {@link RBlock} wrapper
 * (accessed via {@link #block()}); the replacement type is supplied as a
 * resolved {@link RBlockType} (typically {@code minecraft:air}). Convenience
 * accessors {@link #blockType()} / {@link #blockTypeKey()} resolve the
 * destroyed block's current type, while {@link #replacementTypeKey()} exposes
 * the replacement type's key.</p>
 */
public final class BlockDestroyPre extends BaseCancellablePreEvent {
    private final RBlock block;
    private final RBlockType replacementType;


    public BlockDestroyPre(RBlock block, RBlockType replacementType, boolean isCancelled) {
        this.block = Objects.requireNonNull(block, "block");
        this.replacementType = Objects.requireNonNull(replacementType, "replacementType");
        setCancelled(isCancelled);
    }

    public BlockDestroyPre(RBlock block, RKey replacementTypeKey, boolean isCancelled) {
        this(block, RBlockType.require(replacementTypeKey), isCancelled);
    }

    /** The live block being destroyed. */
    public RBlock block() {
        return block;
    }

    /**
     * Convenience accessor resolving the destroyed block's current type via
     * {@code block().requireType()}.
     *
     * @return the block type being destroyed
     */
    public RBlockType blockType() {
        return block.requireType();
    }

    /**
     * Convenience accessor returning the destroyed block's type key via
     * {@code block().typeKey()}.
     *
     * @return the block type key
     */
    public RKey blockTypeKey() {
        return block.typeKey();
    }

    /**
     * The block type that will replace the destroyed block (typically
     * {@code minecraft:air}, but may differ for fluid-destroyed blocks).
     */
    public RBlockType replacementType() {
        return replacementType;
    }

    /** The key of the replacement block type. */
    public RKey replacementTypeKey() {
        return replacementType.key();
    }
}
