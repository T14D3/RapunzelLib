package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public final class WorldLoadPost implements GamePostEvent {
    private final RWorldRef world;

    public WorldLoadPost(RWorldRef world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    public RWorldRef world() {
        return world;
    }
}

