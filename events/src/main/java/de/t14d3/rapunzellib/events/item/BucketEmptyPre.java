package de.t14d3.rapunzellib.events.item;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public final class BucketEmptyPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RWorldRef world;
    private final RBlockPos pos;
    private final String bucketTypeKey;

    public BucketEmptyPre(RPlayer player, RWorldRef world, RBlockPos pos, String bucketTypeKey) {
        this(player, world, pos, bucketTypeKey, false);
    }

    public BucketEmptyPre(RPlayer player, RWorldRef world, RBlockPos pos, String bucketTypeKey, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.bucketTypeKey = Objects.requireNonNull(bucketTypeKey, "bucketTypeKey");
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

    public String bucketTypeKey() {
        return bucketTypeKey;
    }
}
