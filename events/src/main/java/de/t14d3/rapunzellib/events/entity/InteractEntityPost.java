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
 * Post-event fired after a player has interacted with an entity.
 *
 * @param player    the interacting player
 * @param entity    the entity that was interacted with
 * @param snapshot  a snapshot of the entity's state
 * @param cancelled whether the interaction was cancelled
 */
public record InteractEntityPost(
    RPlayer player,
    REntity entity,
    REntitySnapshot snapshot,
    boolean cancelled
) implements GamePostEvent {
    /**
     * Creates an InteractEntityPost from a player, entity, and cancelled state.
     *
     * @param player    the interacting player
     * @param entity    the entity that was interacted with
     * @param cancelled whether the interaction was cancelled
     */
    public InteractEntityPost(RPlayer player, REntity entity, boolean cancelled) {
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

    public Optional<RLivingEntity> livingEntity() {
        return entity.asLivingEntity();
    }
}
