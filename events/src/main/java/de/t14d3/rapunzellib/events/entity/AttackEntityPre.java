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
 * Pre-event fired before a player attacks an entity.
 *
 * <p>This event is cancellable. If denied, the attack will not occur.
 * Contains the player, the target entity, and a snapshot of the entity's state.</p>
 */
public final class AttackEntityPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final REntity entity;
    private final REntitySnapshot snapshot;

    /**
     * Creates a new AttackEntityPre event.
     *
     * @param player the attacking player
     * @param entity the entity being attacked
     */
    public AttackEntityPre(RPlayer player, REntity entity) {
        this(player, entity, false);
    }

    /**
     * Creates a new AttackEntityPre event with cancelled state.
     *
     * @param player      the attacking player
     * @param entity      the entity being attacked
     * @param isCancelled whether the event is initially cancelled
     */
    public AttackEntityPre(RPlayer player, REntity entity, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = entity.snapshot();
        setCancelled(isCancelled);
    }

    /**
     * Returns the attacking player.
     *
     * @return the player
     */
    public RPlayer player() {
        return player;
    }

    /**
     * Returns the entity being attacked.
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
     * Returns a snapshot of the entity's state at the time of the event.
     *
     * @return the entity snapshot
     */
    public REntitySnapshot snapshot() {
        return snapshot;
    }

    /**
     * Returns the world where the attack takes place.
     *
     * @return the world
     */
    public RWorldRef world() {
        return snapshot.world();
    }

    /**
     * Returns the position of the entity.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return snapshot.pos();
    }

    /**
     * Returns the entity type key.
     *
     * @return the entity type key
     */
    public RKey entityTypeKey() {
        return snapshot.entityTypeKey();
    }
}
