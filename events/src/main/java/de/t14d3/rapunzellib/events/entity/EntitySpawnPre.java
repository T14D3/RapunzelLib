package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public final class EntitySpawnPre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey entityTypeKey;
    private final String reason;

    public EntitySpawnPre(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String reason) {
        this(world, pos, entityTypeKey, reason, false);
    }

    public EntitySpawnPre(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String reason, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.entityTypeKey = Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        this.reason = Objects.requireNonNull(reason, "reason");
        setCancelled(isCancelled);
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos pos() {
        return pos;
    }

    public RKey entityTypeKey() {
        return entityTypeKey;
    }

    public String reason() {
        return reason;
    }
}
