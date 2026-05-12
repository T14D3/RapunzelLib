package de.t14d3.rapunzellib.events.interact;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventSnapshot;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.snapshot.RBlockSnapshot;

import java.util.UUID;

/**
 * Immutable snapshot of a block use event for async processing.
 *
 * @param playerUuid the UUID of the player who used the block
 * @param block      the snapshot of the block
 * @param cancelled  whether the interaction was cancelled
 */
public record UseBlockSnapshot(
    UUID playerUuid,
    RBlockSnapshot block,
    boolean cancelled
) implements GameEventSnapshot {
    /**
     * Creates a snapshot from raw data with string key.
     *
     * @param playerUuid    the player UUID
     * @param world         the world reference
     * @param pos           the block position
     * @param blockTypeKey  the block type key as a string
     * @param cancelled     whether the interaction was cancelled
     */
    public UseBlockSnapshot(UUID playerUuid, RWorldRef world, RBlockPos pos, String blockTypeKey, boolean cancelled) {
        this(playerUuid, RBlockSnapshot.of(world, pos, blockTypeKey), cancelled);
    }

    /**
     * Creates a snapshot from raw data.
     *
     * @param playerUuid    the player UUID
     * @param world         the world reference
     * @param pos           the block position
     * @param blockTypeKey  the block type key
     * @param cancelled     whether the interaction was cancelled
     */
    public UseBlockSnapshot(UUID playerUuid, RWorldRef world, RBlockPos pos, RKey blockTypeKey, boolean cancelled) {
        this(playerUuid, RBlockSnapshot.of(world, pos, blockTypeKey), cancelled);
    }

    /**
     * Returns the world from the block snapshot.
     *
     * @return the world
     */
    public RWorldRef world() {
        return block.world();
    }

    /**
     * Returns the position from the block snapshot.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return block.pos();
    }

    /**
     * Returns the block type key from the block snapshot.
     *
     * @return the block type key
     */
    public RKey blockTypeKey() {
        return block.blockTypeKey();
    }

    /**
     * Captures a snapshot from a live block reference.
     *
     * @param playerUuid the player UUID
     * @param block      the live block to snapshot
     * @param cancelled  whether the interaction was cancelled
     * @return the captured snapshot
     */
    public static UseBlockSnapshot capture(UUID playerUuid, RBlock block, boolean cancelled) {
        return new UseBlockSnapshot(playerUuid, RBlockSnapshot.capture(block), cancelled);
    }
}
