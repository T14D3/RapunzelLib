package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

import java.util.Objects;

/**
 * Dispatched before a block forms naturally (e.g., ice, snow, concrete powder).
 *
 * <p>The source-positioned block is supplied as a live {@link RBlock} (the
 * block at the position whose type is about to change), and the resulting type
 * is supplied as a resolved {@link RBlockType} (the type it will form into).
 * Convenience accessors {@link #sourceType()} / {@link #sourceTypeKey()}
 * expose the current/source block's type, while {@link #newType()} /
 * {@link #newTypeKey()} expose the target type. {@link #world()} and
 * {@link #pos()} delegate to {@code block.world().ref()} and
 * {@code block.pos()} respectively.</p>
 */
public final class BlockFormPre extends BaseCancellablePreEvent {
    private final RBlock block;
    private final RBlockType newType;

    public BlockFormPre(RBlock block, RBlockType newType) {
        this(block, newType, false);
    }

    public BlockFormPre(RBlock block, RBlockType newType, boolean isCancelled) {
        this.block = Objects.requireNonNull(block, "block");
        this.newType = Objects.requireNonNull(newType, "newType");
        setCancelled(isCancelled);
    }

    public BlockFormPre(RBlock block, RKey newTypeKey) {
        this(block, RBlockType.require(newTypeKey));
    }

    public BlockFormPre(RBlock block, String newTypeKey) {
        this(block, RBlockType.require(newTypeKey));
    }

    public BlockFormPre(RBlock block, RKey newTypeKey, boolean isCancelled) {
        this(block, RBlockType.require(newTypeKey), isCancelled);
    }

    public BlockFormPre(RBlock block, String newTypeKey, boolean isCancelled) {
        this(block, RBlockType.require(newTypeKey), isCancelled);
    }

    /** The world the forming block belongs to. */
    public RWorldRef world() {
        return block.world().ref();
    }

    /** The position of the forming block. */
    public RBlockPos pos() {
        return block.pos();
    }

    /** The live source-positioned block that will form into a new type. */
    public RBlock block() {
        return block;
    }

    /**
     * Convenience accessor resolving the source block's current type via
     * {@code block().requireType()}.
     *
     * @return the source block type
     */
    public RBlockType sourceType() {
        return block.requireType();
    }

    /** The resulting type the block will form into. */
    public RBlockType newType() {
        return newType;
    }

    /** The key of the source block's type. */
    public RKey sourceTypeKey() {
        return sourceType().key();
    }

    /** The key of the resulting (new) block type. */
    public RKey newTypeKey() {
        return newType.key();
    }
}
