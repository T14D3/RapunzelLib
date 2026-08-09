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
 *
 * <p>The {@link #damager()} is the direct entity that caused the damage
 * (arrow, mob, TNT, …), when the damage is entity-sourced. It is empty for
 * block/environmental damage (fire, lava, fall, …). The Paper bridge skips
 * player-attack damage entirely (those are covered by
 * {@link AttackEntityPre}), so a present damager is never a player.</p>
 */
public final class EntityHurtPre extends BaseCancellablePreEvent {
    private final REntity entity;
    private final REntitySnapshot snapshot;
    private final RKey damageTypeKey;
    private final REntity damager;

    public EntityHurtPre(REntity entity, RKey damageTypeKey) {
        this(entity, damageTypeKey, null, false);
    }

    public EntityHurtPre(REntity entity, RKey damageTypeKey, boolean isCancelled) {
        this(entity, damageTypeKey, null, isCancelled);
    }

    public EntityHurtPre(REntity entity, RKey damageTypeKey, REntity damager, boolean isCancelled) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = entity.snapshot();
        this.damageTypeKey = Objects.requireNonNull(damageTypeKey, "damageTypeKey");
        this.damager = damager;
        setCancelled(isCancelled);
    }

    public EntityHurtPre(REntity entity, String damageTypeKey) {
        this(entity, RKey.of(damageTypeKey), null, false);
    }

    public EntityHurtPre(REntity entity, String damageTypeKey, boolean isCancelled) {
        this(entity, RKey.of(damageTypeKey), null, isCancelled);
    }

    public EntityHurtPre(REntity entity, String damageTypeKey, REntity damager, boolean isCancelled) {
        this(entity, RKey.of(damageTypeKey), damager, isCancelled);
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

    /**
     * Returns the direct entity that caused the damage, when the damage is
     * entity-sourced.
     *
     * <p>Empty for block/environmental damage and on platforms that cannot
     * resolve the damager from their damage event. On Paper the bridge skips
     * player-attack damage (covered by {@link AttackEntityPre}), so a present
     * damager is never a player.</p>
     *
     * @return the damager entity, or empty when unknown or not entity-sourced
     */
    public java.util.Optional<REntity> damager() {
        return java.util.Optional.ofNullable(damager);
    }
}
