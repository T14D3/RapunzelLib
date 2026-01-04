package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;

import java.util.Objects;
import java.util.UUID;

public final class PlayerQuitPost implements GamePostEvent {
    private final UUID uuid;
    private final String name;

    public PlayerQuitPost(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }
}

