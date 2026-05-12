package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;

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

    /**
     * Returns the world from the snapshot.
     *
     * @return the world
     */
    public RWorldRef world() {
        return snapshot.world();
    }

    /**
     * Returns the position from the snapshot.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return snapshot.pos();
    }

    /**
     * Returns the entity type key from the snapshot.
     *
     * @return the entity type key
     */
    public RKey entityTypeKey() {
        return snapshot.entityTypeKey();
    }

    /**
     * Returns the entity as a living entity, if applicable.
     *
     * @return an optional containing the living entity
     */
    public Optional<RLivingEntity> livingEntity() {
        return entity.asLivingEntity();
    }
}
