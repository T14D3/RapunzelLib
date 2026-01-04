package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public final class ChunkUnloadPost implements GamePostEvent {
    private final RWorldRef world;
    private final int chunkX;
    private final int chunkZ;

    public ChunkUnloadPost(RWorldRef world, int chunkX, int chunkZ) {
        this.world = Objects.requireNonNull(world, "world");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public RWorldRef world() {
        return world;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }
}

