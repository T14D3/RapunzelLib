package de.t14d3.rapunzellib.events.item;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public final class BucketEntityPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RWorldRef world;
    private final RBlockPos pos;
    private final String entityTypeKey;

    public BucketEntityPre(RPlayer player, RWorldRef world, RBlockPos pos, String entityTypeKey) {
        this(player, world, pos, entityTypeKey, false);
    }

    public BucketEntityPre(RPlayer player, RWorldRef world, RBlockPos pos, String entityTypeKey, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.entityTypeKey = Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        this.cancelled = isCancelled;
    }

    public RPlayer player() {
        return player;
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
}
