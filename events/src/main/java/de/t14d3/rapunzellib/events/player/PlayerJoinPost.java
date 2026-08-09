package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired after a player has joined the server.
 *
 * @param uuid the player's UUID
 * @param name the player's name
 */
public record PlayerJoinPost(UUID uuid, String name) implements GamePostEvent {

    public PlayerJoinPost {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
    }
}
