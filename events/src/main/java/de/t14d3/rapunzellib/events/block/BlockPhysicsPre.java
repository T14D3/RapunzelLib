package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Dispatched before block physics are applied (e.g., sand/gravel falling, redstone updates).
 *
 * <p>The {@link #block()} identifies the live block undergoing physics (the
 * neighbor being checked), while {@link #changedType()} identifies the block
 * type that changed and triggered this physics update (e.g., the block that
 * was removed, causing sand above to fall). Convenience accessors
 * {@link #blockType()} / {@link #blockTypeKey()} resolve the undergoing block's
 * type, while {@link #changedTypeKey()} exposes the trigger type's key.
 * {@link #world()} and {@link #pos()} delegate to {@code block.world().ref()}
 * and {@code block.pos()} respectively.</p>
 */
public final class BlockPhysicsPre extends BaseCancellablePreEvent {

    private final @NotNull RBlock block;
    private final @NotNull RBlockType changedType;

    /**
     * @param block       the live block undergoing physics
     * @param changedType the block type that changed, triggering this physics update
     */
    public BlockPhysicsPre(@NotNull RBlock block, @NotNull RBlockType changedType) {
        this(block, changedType, false);
    }

    /**
     * @param block       the live block undergoing physics
     * @param changedType the block type that changed, triggering this physics update
     * @param isCancelled initial cancelled state
     */
    public BlockPhysicsPre(@NotNull RBlock block, @NotNull RBlockType changedType, boolean isCancelled) {
        this.block = Objects.requireNonNull(block, "block");
        this.changedType = Objects.requireNonNull(changedType, "changedType");
        setCancelled(isCancelled);
    }

    /**
     * @param block          the live block undergoing physics
     * @param changedTypeKey the block type that changed, triggering this physics update (as a key)
     */
    public BlockPhysicsPre(@NotNull RBlock block, @NotNull RKey changedTypeKey) {
        this(block, RBlockType.require(changedTypeKey));
    }

    /**
     * @param block          the live block undergoing physics
     * @param changedTypeKey the block type that changed, as a string key
     */
    public BlockPhysicsPre(@NotNull RBlock block, @NotNull String changedTypeKey) {
        this(block, RBlockType.require(changedTypeKey));
    }

    /**
     * @param block          the live block undergoing physics
     * @param changedTypeKey the block type that changed, triggering this physics update (as a key)
     * @param isCancelled    initial cancelled state
     */
    public BlockPhysicsPre(@NotNull RBlock block, @NotNull RKey changedTypeKey, boolean isCancelled) {
        this(block, RBlockType.require(changedTypeKey), isCancelled);
    }

    /**
     * @param block          the live block undergoing physics
     * @param changedTypeKey the block type that changed, as a string key
     * @param isCancelled    initial cancelled state
     */
    public BlockPhysicsPre(@NotNull RBlock block, @NotNull String changedTypeKey, boolean isCancelled) {
        this(block, RBlockType.require(changedTypeKey), isCancelled);
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** The world the physics update is occurring in. */
    public @NotNull RWorldRef world() {
        return block.world().ref();
    }

    /** The position of the block undergoing physics (the neighbor being checked). */
    public @NotNull RBlockPos pos() {
        return block.pos();
    }

    /** The live block undergoing physics (e.g. {@code minecraft:sand} when sand
     * checks whether its supporting block has been removed). */
    public @NotNull RBlock block() {
        return block;
    }

    /**
     * Convenience accessor resolving the undergoing block's current type via
     * {@code block().requireType()}.
     */
    public @NotNull RBlockType blockType() {
        return block.requireType();
    }

    /**
     * Convenience accessor returning the undergoing block's type key via
     * {@code block().typeKey()}.
     */
    public @NotNull RKey blockTypeKey() {
        return block.typeKey();
    }

    /**
     * The block type that changed, <em>triggering</em> this physics update
     * (e.g. {@code minecraft:air} if a support block was removed, causing sand
     * above to check its physics).
     */
    public @NotNull RBlockType changedType() {
        return changedType;
    }

    /**
     * The key of the block type that changed, triggering this physics update.
     */
    public @NotNull RKey changedTypeKey() {
        return changedType.key();
    }
}
