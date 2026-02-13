package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Dispatched before block physics are applied (e.g., sand/gravel falling, redstone updates).
 */
public final class BlockPhysicsPre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey blockTypeKey;
    private final int changedTypeId;

    public BlockPhysicsPre(RWorldRef world, RBlockPos pos, RKey blockTypeKey, int changedTypeId) {
        this(world, pos, blockTypeKey, changedTypeId, false);
    }

    public BlockPhysicsPre(RWorldRef world, RBlockPos pos, String blockTypeKey, int changedTypeId) {
        this(world, pos, RKey.of(blockTypeKey), changedTypeId);
    }

    public BlockPhysicsPre(RWorldRef world, RBlockPos pos, RKey blockTypeKey, int changedTypeId, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.blockTypeKey = Objects.requireNonNull(blockTypeKey, "blockTypeKey");
        this.changedTypeId = changedTypeId;
        setCancelled(isCancelled);
    }

    public BlockPhysicsPre(RWorldRef world, RBlockPos pos, String blockTypeKey, int changedTypeId, boolean isCancelled) {
        this(world, pos, RKey.of(blockTypeKey), changedTypeId, isCancelled);
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos pos() {
        return pos;
    }

    /**
     * The block type undergoing physics.
     */
    public RKey blockTypeKey() {
        return blockTypeKey;
    }

    /**
     * The type ID of the block that changed, triggering this physics update.
     * Returns -1 if not applicable.
     */
    public int changedTypeId() {
        return changedTypeId;
    }
}
