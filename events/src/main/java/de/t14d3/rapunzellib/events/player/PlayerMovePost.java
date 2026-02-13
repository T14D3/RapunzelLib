package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * Post-event fired after a player has moved.
 *
 * <p>This event is immutable and contains information about the completed movement.
 * It can be used for logging, region entries/exits, and other post-movement processing.
 */
public record PlayerMovePost(UUID uuid, String name, RLocation from, RLocation to) implements GamePostEvent {
    public PlayerMovePost(UUID uuid, String name, RLocation from, RLocation to) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }
}
