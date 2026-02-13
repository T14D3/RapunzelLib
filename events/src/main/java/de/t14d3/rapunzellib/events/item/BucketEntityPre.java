package de.t14d3.rapunzellib.events.item;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public final class BucketEntityPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey entityTypeKey;

    public BucketEntityPre(RPlayer player, RWorldRef world, RBlockPos pos, RKey entityTypeKey) {
        this(player, world, pos, entityTypeKey, false);
    }

    public BucketEntityPre(RPlayer player, RWorldRef world, RBlockPos pos, RKey entityTypeKey, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.entityTypeKey = Objects.requireNonNull(entityTypeKey, "entityTypeKey");
        setCancelled(isCancelled);
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

    public RKey entityTypeKey() {
        return entityTypeKey;
    }
}
