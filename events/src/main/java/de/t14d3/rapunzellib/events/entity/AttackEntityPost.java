package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import de.t14d3.rapunzellib.registry.REntityType;

import java.util.Optional;

/**
 * Post-event fired after a player has attacked an entity.
 *
 * @param player    the attacking player
 * @param entity    the entity that was attacked
 * @param snapshot  a snapshot of the entity's state
 * @param cancelled whether the attack was cancelled
 */
public record AttackEntityPost(
    RPlayer player,
    REntity entity,
    REntitySnapshot snapshot,
    boolean cancelled
) implements GamePostEvent {
    /**
     * Creates an AttackEntityPost from a player, entity, and cancelled state.
     *
     * @param player    the attacking player
     * @param entity    the entity that was attacked
     * @param cancelled whether the attack was cancelled
     */
    public AttackEntityPost(RPlayer player, REntity entity, boolean cancelled) {
        this(player, entity, entity.snapshot(), cancelled);
    }

    public RWorldRef world() {
        return snapshot.world();
    }

    public RBlockPos pos() {
        return snapshot.pos();
    }

    public RKey entityTypeKey() {
        return snapshot.entityTypeKey();
    }

    /**
     * Returns the typed entity type wrapper, resolved from the live entity.
     *
     * @return the entity type
     */
    public REntityType entityType() {
        return entity.requireType();
    }

    public Optional<RLivingEntity> livingEntity() {
        return entity.asLivingEntity();
    }
}
