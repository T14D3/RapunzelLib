package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired after a player has quit the server.
 *
 * @param uuid the player's UUID
 * @param name the player's name
 */
public record PlayerQuitPost(UUID uuid, String name) implements GamePostEvent {
    
    public PlayerQuitPost(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
    }
}
