package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventSnapshot;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.snapshot.RBlockSnapshot;

import java.util.UUID;

public record BlockPlaceSnapshot(
    UUID playerUuid,
    RBlockSnapshot block,
    boolean cancelled
) implements GameEventSnapshot {
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

    public static BlockPlaceSnapshot capture(UUID playerUuid, RBlock block, boolean cancelled) {
        return new BlockPlaceSnapshot(playerUuid, RBlockSnapshot.capture(block), cancelled);
    }
}
