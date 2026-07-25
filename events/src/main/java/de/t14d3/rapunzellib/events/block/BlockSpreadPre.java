package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

import java.util.Objects;

/**
 * Dispatched before a block spreads to another location (e.g., fire, mushrooms).
 *
 * <p>Both the target block and the donor {@code source} block are supplied as
 * live {@link RBlock} wrappers - the spread event always has both blocks
 * alive at the bridge layer, so no stringly-typed constructors are provided.
 * Convenience accessors {@link #newType()} / {@link #newTypeKey()} expose the
 * target's resolved type, and {@link #sourceType()} / {@link #sourceTypeKey()}
 * expose the donor's type. {@link #world()} and {@link #pos()} delegate to
 * {@code block.world().ref()} and {@code block.pos()} respectively.</p>
 */
public final class BlockSpreadPre extends BaseCancellablePreEvent {
    private final RBlock block;
    private final RBlock source;

    public BlockSpreadPre(RBlock block, RBlock source) {
        this(block, source, false);
    }

    public BlockSpreadPre(RBlock block, RBlock source, boolean isCancelled) {
        this.block = Objects.requireNonNull(block, "block");
        this.source = Objects.requireNonNull(source, "source");
        setCancelled(isCancelled);
    }

    /** The world the spreading-target block belongs to. */
    public RWorldRef world() {
        return block.world().ref();
    }

    /** The position of the spreading-target block. */
    public RBlockPos pos() {
        return block.pos();
    }

    /** The live target block (e.g. dirt becoming grass). */
    public RBlock block() {
        return block;
    }

    /** The live donor/source block (e.g. existing grass). */
    public RBlock source() {
        return source;
    }

    /**
     * Convenience accessor resolving the target block's type via
     * {@code block().requireType()}.
     *
     * @return the new (target) block type
     */
    public RBlockType newType() {
        return block.requireType();
    }

    /**
     * Convenience accessor resolving the donor block's type via
     * {@code source().requireType()}.
     *
     * @return the source block type
     */
    public RBlockType sourceType() {
        return source.requireType();
    }

    /** The key of the new (target) block type. */
    public RKey newTypeKey() {
        return newType().key();
    }

    /** The key of the source block type. */
    public RKey sourceTypeKey() {
        return sourceType().key();
    }
}
