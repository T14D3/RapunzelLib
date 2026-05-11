package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;

import java.util.Objects;
import java.util.UUID;

public record PlayerQuitPre(UUID uuid, String name) implements GamePostEvent {
    public PlayerQuitPre(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
    }
}
