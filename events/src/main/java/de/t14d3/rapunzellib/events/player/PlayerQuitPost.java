package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;

import java.util.Objects;
import java.util.UUID;

public record PlayerQuitPost(UUID uuid, String name) implements GamePostEvent {
    public PlayerQuitPost(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
    }
}

