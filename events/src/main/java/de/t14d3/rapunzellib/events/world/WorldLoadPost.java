package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Fired after a world has been loaded.
 *
 * @param world the world that was loaded
 */
public record WorldLoadPost(RWorldRef world) implements GamePostEvent {
    /**
     * Creates a new world load post-event.
     *
     * @param world the world reference
     */
    public WorldLoadPost(RWorldRef world) {
        this.world = Objects.requireNonNull(world, "world");
    }
}

