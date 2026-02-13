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

public final class AttackEntityPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final REntity entity;
    private final REntitySnapshot snapshot;

    public AttackEntityPre(RPlayer player, REntity entity) {
        this(player, entity, false);
    }

    public AttackEntityPre(RPlayer player, REntity entity, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.snapshot = entity.snapshot();
        setCancelled(isCancelled);
    }

    public RPlayer player() {
        return player;
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
}
