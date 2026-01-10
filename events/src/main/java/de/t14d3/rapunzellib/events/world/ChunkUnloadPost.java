package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public record ChunkUnloadPost(RWorldRef world, int chunkX, int chunkZ) implements GamePostEvent {
    public ChunkUnloadPost(RWorldRef world, int chunkX, int chunkZ) {
        this.world = Objects.requireNonNull(world, "world");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
}

