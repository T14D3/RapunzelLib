package de.t14d3.rapunzellib.events.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.GameEventSnapshot;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.snapshot.REntitySnapshot;

import java.util.UUID;

public record EntitySpawnSnapshot(
 REntitySnapshot entity,
 String reason,
 boolean cancelled
) implements GameEventSnapshot {
    public EntitySpawnSnapshot(RWorldRef world, RBlockPos pos, RKey entityTypeKey, String reason, boolean cancelled) {
        this(REntitySnapshot.of(UUID.randomUUID(), world, pos, entityTypeKey), reason, cancelled);
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

    public static EntitySpawnSnapshot capture(REntity entity, String reason, boolean cancelled) {
        return new EntitySpawnSnapshot(entity.snapshot(), reason, cancelled);
    }
}
