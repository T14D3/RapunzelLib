package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;
import org.jetbrains.annotations.NotNull;

/**
 * Post-event fired after a neighbor-physics update has run.
 *
 * <p>Fires at the same point as {@link BlockPhysicsPre}: the
 * {@code NeighborUpdater.executeUpdate} funnel, once per affected neighbor
 * block. {@code cancelled} is {@code true} when a pre-listener denied the
 * update (in which case the physics reaction was skipped).</p>
 *
 * <p>Components are the live {@link #block()} undergoing physics, the
 * {@link #changedType()} that triggered the update, and the cancellation flag.
 * Convenience accessors {@link #blockType()} / {@link #blockTypeKey()} resolve
 * the undergoing block's type, while {@link #changedTypeKey()} exposes the
 * trigger type's key. {@link #world()} and {@link #pos()} delegate to
 * {@code block.world().ref()} and {@code block.pos()} respectively.</p>
 *
 * @param block       the live block undergoing physics
 * @param changedType the block type that changed, triggering this physics update
 * @param cancelled   whether the physics update was cancelled
 */
public record BlockPhysicsPost(
        @NotNull RBlock block,
        @NotNull RBlockType changedType,
        boolean cancelled
) implements GamePostEvent {
    public BlockPhysicsPost {
        java.util.Objects.requireNonNull(block, "block");
        java.util.Objects.requireNonNull(changedType, "changedType");
    }

    /** The world the physics update occurred in. */
    public @NotNull RWorldRef world() {
        return block.world().ref();
    }

    /** The position of the block undergoing physics. */
    public @NotNull RBlockPos pos() {
        return block.pos();
    }

    /**
     * Convenience accessor resolving the undergoing block's current type via
     * {@code block.requireType()}.
     */
    public @NotNull RBlockType blockType() {
        return block.requireType();
    }

    /**
     * Convenience accessor returning the undergoing block's type key via
     * {@code block.typeKey()}.
     */
    public @NotNull RKey blockTypeKey() {
        return block.typeKey();
    }

    /** The key of the block type that changed, triggering this physics update. */
    public @NotNull RKey changedTypeKey() {
        return changedType.key();
    }
}
