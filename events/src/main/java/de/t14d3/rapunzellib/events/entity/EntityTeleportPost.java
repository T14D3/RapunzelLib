package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;

import java.util.Objects;
import java.util.UUID;

public record EntityTeleportPost(UUID uuid, RKey entityTypeKey, RLocation from, RLocation to) implements GamePostEvent {
    public EntityTeleportPost(UUID uuid, RKey entityTypeKey, RLocation from, RLocation to) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.entityTypeKey = Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }
}
