package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;

import java.util.Objects;

/**
 * Pre-event fired before an entity takes damage.
 *
 * <p>This event is cancellable. If denied, the entity will not take damage.
 * Contains the entity, damage type, and a snapshot of the entity's state.</p>
 */
public final class EntityHurtPre extends BaseCancellablePreEvent {
    private final REntity entity;
    private final REntitySnapshot snapshot;
    private final String damageTypeKey;

    /**
     * Creates a new EntityHurtPre event.
     *
     * @param entity        the entity being hurt
     * @param damageTypeKey the damage type key
     */
    public EntityHurtPre(REntity entity, String damageTypeKey) {
        this(entity, damageTypeKey, false);
    }

    /**
     * Creates a new EntityHurtPre event with cancelled state.
     *
     * @param entity        the entity being hurt
     * @param damageTypeKey the damage type key
     * @param isCancelled   whether the event is initially cancelled
     */
    public EntityHurtPre(REntity entity, String damageTypeKey, boolean isCancelled) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = entity.snapshot();
        this.damageTypeKey = Objects.requireNonNull(damageTypeKey, "damageTypeKey");
        setCancelled(isCancelled);
    }

    /**
     * Returns the entity taking damage.
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

    /**
     * Returns the damage type key.
     *
     * @return the damage type key
     */
    public String damageTypeKey() {
        return damageTypeKey;
    }
}
