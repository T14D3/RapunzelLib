package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RPlayer;

import java.util.Objects;

/**
 * Post-event fired after an entity has been tamed.
 *
 * <p>Carries the tamer as a live {@link RPlayer} reference and the tamed
 * entity as a live {@link REntity} reference. On the Paper bridge the tamer
 * is the player who performed the tame action (the event fires synchronously
 * during the tame interaction, so the player is online).</p>
 *
 * @param tamer  the player who tamed the entity
 * @param entity the tamed entity
 */
public record EntityTamePost(RPlayer tamer, REntity entity) implements GamePostEvent {

    public EntityTamePost {
        Objects.requireNonNull(tamer, "tamer");
        Objects.requireNonNull(entity, "entity");
    }
}
