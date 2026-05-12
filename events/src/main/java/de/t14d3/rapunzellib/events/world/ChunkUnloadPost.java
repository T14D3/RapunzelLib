package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Fired after a chunk has been unloaded from a world.
 *
 * @param world the world the chunk belongs to
 * @param chunkX the chunk's x-coordinate
 * @param chunkZ the chunk's z-coordinate
 */
public record ChunkUnloadPost(RWorldRef world, int chunkX, int chunkZ) implements GamePostEvent {
    /**
     * Creates a new chunk unload post-event.
     *
     * @param world the world reference
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     */
    public ChunkUnloadPost(RWorldRef world, int chunkX, int chunkZ) {
        this.world = Objects.requireNonNull(world, "world");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
}

