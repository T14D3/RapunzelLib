package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.RBlockPos;

public record BlockPhysicsPost(RWorldRef world, RBlockPos pos, RKey blockTypeKey, int changedTypeId, boolean cancelled) implements GamePostEvent {
    public BlockPhysicsPost(RWorldRef world, RBlockPos pos, String blockTypeKey, int changedTypeId, boolean cancelled) {
        this(world, pos, RKey.of(blockTypeKey), changedTypeId, cancelled);
    }
}
