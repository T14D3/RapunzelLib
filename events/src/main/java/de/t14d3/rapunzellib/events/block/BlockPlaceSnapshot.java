package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.GameEventSnapshot;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.UUID;

public record BlockPlaceSnapshot(
    UUID playerUuid,
    RWorldRef world,
    RBlockPos pos,
    String blockTypeKey,
    boolean cancelled
) implements GameEventSnapshot {
}

