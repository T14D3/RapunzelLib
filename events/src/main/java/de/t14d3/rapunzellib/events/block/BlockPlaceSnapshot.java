package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventSnapshot;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.snapshot.RBlockSnapshot;

import java.util.UUID;

/**
 * Immutable snapshot of a block place event for async processing.
 *
 * <p>Contains the player UUID, a snapshot of the placed block, and the cancelled state.</p>
 *
 * @param playerUuid the UUID of the player who placed the block
 * @param block      the snapshot of the placed block
 * @param cancelled  whether the placement was cancelled
 */
public record BlockPlaceSnapshot(
    UUID playerUuid,
    RBlockSnapshot block,
    boolean cancelled
) implements GameEventSnapshot {
    /**
     * Creates a snapshot from raw world/position/type data with string key.
     *
     * @param playerUuid    the player UUID
     * @param world         the world reference
     * @param pos           the block position
     * @param blockTypeKey  the block type key as a string
     * @param cancelled     whether the placement was cancelled
     */
    public BlockPlaceSnapshot(UUID playerUuid, RWorldRef world, RBlockPos pos, String blockTypeKey, boolean cancelled) {
        this(playerUuid, RBlockSnapshot.of(world, pos, blockTypeKey), cancelled);
    }

    public BlockPlaceSnapshot(UUID playerUuid, RWorldRef world, RBlockPos pos, RKey blockTypeKey, boolean cancelled) {
        this(playerUuid, RBlockSnapshot.of(world, pos, blockTypeKey), cancelled);
    }

    public RWorldRef world() {
        return block.world();
    }

    public RBlockPos pos() {
        return block.pos();
    }

    public RKey blockTypeKey() {
        return block.blockTypeKey();
    }

    /**
     * Captures a snapshot from a live block reference.
     *
     * @param playerUuid the player UUID
     * @param block      the live block to snapshot
     * @param cancelled  whether the placement was cancelled
     * @return the captured snapshot
     */
    public static BlockPlaceSnapshot capture(UUID playerUuid, RBlock block, boolean cancelled) {
        return new BlockPlaceSnapshot(playerUuid, RBlockSnapshot.capture(block), cancelled);
    }
}
