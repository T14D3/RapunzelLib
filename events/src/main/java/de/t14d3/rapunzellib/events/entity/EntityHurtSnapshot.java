package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventSnapshot;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;

import java.util.UUID;

public record EntityHurtSnapshot(
 REntitySnapshot entity,
 String damageTypeKey,
 boolean cancelled
) implements GameEventSnapshot {
    public EntityHurtSnapshot(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String damageTypeKey, boolean cancelled) {
        this(REntitySnapshot.of(UUID.randomUUID(), world, pos, entityTypeKey), damageTypeKey, cancelled);
    }

    public RWorldRef world() {
        return entity.world();
    }

    public RBlockPos pos() {
        return entity.pos();
    }

    public RKey entityTypeKey() {
        return entity.entityTypeKey();
    }

    public static EntityHurtSnapshot capture(REntity entity, String damageTypeKey, boolean cancelled) {
        return new EntityHurtSnapshot(entity.snapshot(), damageTypeKey, cancelled);
    }
}
