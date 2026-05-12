package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * Pre-event fired before a player quits the server.
 *
 * @param uuid the UUID of the quitting player
 * @param name the name of the quitting player
 */
public record PlayerQuitPre(UUID uuid, String name) implements GamePostEvent {
    public PlayerQuitPre(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
    }
}
