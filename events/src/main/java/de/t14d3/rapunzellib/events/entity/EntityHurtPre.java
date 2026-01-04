package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public final class EntityHurtPre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final String entityTypeKey;
    private final String damageTypeKey;

    public EntityHurtPre(RWorldRef world, RBlockPos pos, String entityTypeKey, String damageTypeKey) {
        this(world, pos, entityTypeKey, damageTypeKey, false);
    }

    public EntityHurtPre(RWorldRef world, RBlockPos pos, String entityTypeKey, String damageTypeKey, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.entityTypeKey = Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        this.damageTypeKey = Objects.requireNonNull(damageTypeKey, "damageTypeKey");
        this.cancelled = isCancelled;
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos pos() {
        return pos;
    }

    public String entityTypeKey() {
        return entityTypeKey;
    }

    public String damageTypeKey() {
        return damageTypeKey;
    }
}
