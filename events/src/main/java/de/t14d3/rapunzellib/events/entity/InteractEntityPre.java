package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;

import java.util.Objects;

/**
 * Pre-event fired before a player interacts with an entity.
 *
 * <p>This event is cancellable. If denied, the interaction will not occur.
 * Contains the player, the target entity, and a snapshot of the entity's state.</p>
 */
public final class InteractEntityPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final REntity entity;
    private final REntitySnapshot snapshot;

    /**
     * Creates a new InteractEntityPre event.
     *
     * @param player the interacting player
     * @param entity the entity being interacted with
     */
    public InteractEntityPre(RPlayer player, REntity entity) {
        this(player, entity, false);
    }

    /**
     * Creates a new InteractEntityPre event with cancelled state.
     *
     * @param player      the interacting player
     * @param entity      the entity being interacted with
     * @param isCancelled whether the event is initially cancelled
     */
    public InteractEntityPre(RPlayer player, REntity entity, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = entity.snapshot();
        setCancelled(isCancelled);
    }

    /**
     * Returns the interacting player.
     *
     * @return the player
     */
    public RPlayer player() {
        return player;
    }

    /**
     * Returns the entity being interacted with.
     *
     * @return the entity
     */
    public REntity entity() {
        return entity;
    }

    /**
     * Returns the entity as a living entity, if applicable.
     *
     * @return an optional containing the living entity
     */
    public java.util.Optional<RLivingEntity> livingEntity() {
        return entity.asLivingEntity();
    }

    /**
     * Returns the entity snapshot.
     *
     * @return the snapshot
     */
    public REntitySnapshot snapshot() {
        return snapshot;
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
}
