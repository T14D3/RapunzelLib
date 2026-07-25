package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;
import de.t14d3.rapunzellib.registry.REntityType;

import java.util.Objects;

/**
 * Pre-event fired before an entity takes damage.
 *
 * <p>This event is cancellable. If denied, the entity will not take damage.
 * Contains the entity, damage type, and a snapshot of the entity's state.</p>
 *
 * <p>The damage type is carried as an interned {@link RKey}. A typed
 * {@code RDamageType} wrapper is not available yet.</p>
 */
public final class EntityHurtPre extends BaseCancellablePreEvent {
    private final REntity entity;
    private final REntitySnapshot snapshot;
    private final RKey damageTypeKey;

    public EntityHurtPre(REntity entity, RKey damageTypeKey) {
        this(entity, damageTypeKey, false);
    }

    public EntityHurtPre(REntity entity, RKey damageTypeKey, boolean isCancelled) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = entity.snapshot();
        this.damageTypeKey = Objects.requireNonNull(damageTypeKey, "damageTypeKey");
        setCancelled(isCancelled);
    }

    public EntityHurtPre(REntity entity, String damageTypeKey) {
        this(entity, RKey.of(damageTypeKey), false);
    }

    public EntityHurtPre(REntity entity, String damageTypeKey, boolean isCancelled) {
        this(entity, RKey.of(damageTypeKey), isCancelled);
    }

    public REntity entity() {
        return entity;
    }

    public java.util.Optional<RLivingEntity> livingEntity() {
        return entity.asLivingEntity();
    }

    public REntitySnapshot snapshot() {
        return snapshot;
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

    /**
     * Returns the damage type key.
     *
     * @return the damage type key
     */
    public RKey damageTypeKey() {
        return damageTypeKey;
    }
}
