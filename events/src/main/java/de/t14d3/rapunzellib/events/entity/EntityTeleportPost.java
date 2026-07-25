package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.registry.REntityType;

import java.util.Objects;
import java.util.UUID;

/**
 * Post-event fired after an entity has teleported.
 *
 * @param entity the entity that teleported
 * @param from   the location the entity teleported from
 * @param to     the location the entity teleported to
 */
public record EntityTeleportPost(REntity entity, RLocation from, RLocation to) implements GamePostEvent {
    public EntityTeleportPost(REntity entity, RLocation from, RLocation to) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }

    /**
     * Returns the UUID of the entity that teleported.
     *
     * @return the entity UUID
     */
    public UUID uuid() {
        return entity.uuid();
    }

    /**
     * Returns the entity type key.
     *
     * @return the entity type key
     */
    public RKey entityTypeKey() {
        return entity.typeKey();
    }

    /**
     * Returns the typed entity type wrapper, resolved from the live entity.
     *
     * @return the entity type
     */
    public REntityType entityType() {
        return entity.requireType();
    }
}
