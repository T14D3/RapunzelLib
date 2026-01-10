package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public record WorldLoadPost(RWorldRef world) implements GamePostEvent {
    public WorldLoadPost(RWorldRef world) {
        this.world = Objects.requireNonNull(world, "world");
    }
}

