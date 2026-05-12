package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * Post-event fired after an entity has moved.
 *
 * @param uuid          the UUID of the entity
 * @param entityTypeKey the entity type key
 * @param from          the location the entity moved from
 * @param to            the location the entity moved to
 */
public record EntityMovePost(UUID uuid, RKey entityTypeKey, RLocation from, RLocation to) implements GamePostEvent {
    public EntityMovePost(UUID uuid, RKey entityTypeKey, RLocation from, RLocation to) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.entityTypeKey = Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }
}
