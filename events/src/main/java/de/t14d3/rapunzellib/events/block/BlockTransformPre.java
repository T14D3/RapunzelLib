package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

import java.util.Objects;

/**
 * Dispatched before a block transforms (e.g., oxidation, waxing, etc.).
 *
 * <p>The original block is supplied as a live {@link RBlock} (the block being
 * transformed) and the target is supplied as a resolved {@link RBlockType}
 * (the type the block will become - there is no live block for it yet).
 * Convenience accessors {@link #originalType()} / {@link #originalTypeKey()}
 * expose the original block's current type, while {@link #transformedType()}
 * / {@link #transformedTypeKey()} expose the target type. {@link #world()} and
 * {@link #pos()} delegate to {@code block.world().ref()} and
 * {@code block.pos()} respectively.</p>
 */
public final class BlockTransformPre extends BaseCancellablePreEvent {
    private final RBlock block;
    private final RBlockType transformedType;

    public BlockTransformPre(RBlock block, RBlockType transformedType) {
        this(block, transformedType, false);
    }

    public BlockTransformPre(RBlock block, RBlockType transformedType, boolean isCancelled) {
        this.block = Objects.requireNonNull(block, "block");
        this.transformedType = Objects.requireNonNull(transformedType, "transformedType");
        setCancelled(isCancelled);
    }

    public BlockTransformPre(RBlock block, RKey transformedTypeKey) {
        this(block, RBlockType.require(transformedTypeKey));
    }

    public BlockTransformPre(RBlock block, String transformedTypeKey) {
        this(block, RBlockType.require(transformedTypeKey));
    }

    public BlockTransformPre(RBlock block, RKey transformedTypeKey, boolean isCancelled) {
        this(block, RBlockType.require(transformedTypeKey), isCancelled);
    }

    public BlockTransformPre(RBlock block, String transformedTypeKey, boolean isCancelled) {
        this(block, RBlockType.require(transformedTypeKey), isCancelled);
    }

    /** The world the transformed block belongs to. */
    public RWorldRef world() {
        return block.world().ref();
    }

    /** The position of the transformed block. */
    public RBlockPos pos() {
        return block.pos();
    }

    /** The original (live) block being transformed. */
    public RBlock block() {
        return block;
    }

    /**
     * Convenience accessor resolving the original block's current type via
     * {@code block().requireType()}.
     *
     * @return the original block type
     */
    public RBlockType originalType() {
        return block.requireType();
    }

    /** The target type the block will be transformed into. */
    public RBlockType transformedType() {
        return transformedType;
    }

    /** The key of the original block's type. */
    public RKey originalTypeKey() {
        return originalType().key();
    }

    /** The key of the transformed (target) block type. */
    public RKey transformedTypeKey() {
        return transformedType.key();
    }
}
