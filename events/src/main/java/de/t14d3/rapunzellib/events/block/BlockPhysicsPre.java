package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Dispatched before block physics are applied (e.g., sand/gravel falling, redstone updates).
 *
 * <p>The {@link #blockTypeKey()} identifies the block undergoing physics (the neighbor being
 * checked), while {@link #changedTypeKey()} identifies the block type that changed and triggered
 * this physics update (e.g., the block that was removed, causing sand above to fall).</p>
 */
public final class BlockPhysicsPre extends BaseCancellablePreEvent {

    private final @NotNull RWorldRef world;
    private final @NotNull RBlockPos pos;
    private final @NotNull RKey blockTypeKey;
    private final @NotNull RKey changedTypeKey;

    /**
     * @param world          the world reference
     * @param pos            the position of the block undergoing physics
     * @param blockTypeKey   the block type undergoing physics (e.g. {@code minecraft:sand})
     * @param changedTypeKey the block type that changed, triggering this physics update
     */
    public BlockPhysicsPre(
            @NotNull RWorldRef world,
            @NotNull RBlockPos pos,
            @NotNull RKey blockTypeKey,
            @NotNull RKey changedTypeKey
    ) {
        this(world, pos, blockTypeKey, changedTypeKey, false);
    }

    /**
     * @param world          the world reference
     * @param pos            the position of the block undergoing physics
     * @param blockTypeKey   the block type undergoing physics (e.g. {@code minecraft:sand})
     * @param changedTypeKey the block type that changed, triggering this physics update
     * @param isCancelled    initial cancelled state
     */
    public BlockPhysicsPre(
            @NotNull RWorldRef world,
            @NotNull RBlockPos pos,
            @NotNull RKey blockTypeKey,
            @NotNull RKey changedTypeKey,
            boolean isCancelled
    ) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.blockTypeKey = Objects.requireNonNull(blockTypeKey, "blockTypeKey");
        this.changedTypeKey = Objects.requireNonNull(changedTypeKey, "changedTypeKey");
        setCancelled(isCancelled);
    }

    /**
     * @param world          the world reference
     * @param pos            the position of the block undergoing physics
     * @param blockTypeKey   the block type undergoing physics, as a string key
     * @param changedTypeKey the block type that changed, as a string key
     */
    public BlockPhysicsPre(
            @NotNull RWorldRef world,
            @NotNull RBlockPos pos,
            @NotNull String blockTypeKey,
            @NotNull String changedTypeKey
    ) {
        this(world, pos, RKey.of(blockTypeKey), RKey.of(changedTypeKey));
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** The world the physics update is occurring in. */
    public @NotNull RWorldRef world() {
        return world;
    }

    /** The position of the block undergoing physics (the neighbor being checked). */
    public @NotNull RBlockPos pos() {
        return pos;
    }

    /**
     * The block type undergoing physics (e.g. {@code minecraft:sand} when sand
     * checks whether its supporting block has been removed).
     */
    public @NotNull RKey blockTypeKey() {
        return blockTypeKey;
    }

    /**
     * The block type that changed, <em>triggering</em> this physics update
     * (e.g. {@code minecraft:air} if a support block was removed, causing sand
     * above to check its physics).
     */
    public @NotNull RKey changedTypeKey() {
        return changedTypeKey;
    }
}
