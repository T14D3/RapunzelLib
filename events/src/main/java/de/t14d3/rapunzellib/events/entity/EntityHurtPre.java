package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLivingEntity;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;

import java.util.Objects;

public final class EntityHurtPre extends BaseCancellablePreEvent {
    private final REntity entity;
    private final REntitySnapshot snapshot;
    private final String damageTypeKey;

    public EntityHurtPre(REntity entity, String damageTypeKey) {
        this(entity, damageTypeKey, false);
    }

    public EntityHurtPre(REntity entity, String damageTypeKey, boolean isCancelled) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = entity.snapshot();
        this.damageTypeKey = Objects.requireNonNull(damageTypeKey, "damageTypeKey");
        setCancelled(isCancelled);
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

    public String damageTypeKey() {
        return damageTypeKey;
    }
}
