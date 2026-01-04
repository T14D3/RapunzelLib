package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.List;
import java.util.Objects;

public final class ExplosionPre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos origin;
    private final String sourceTypeKey;
    private final List<RBlockPos> affectedBlocks;

    public ExplosionPre(RWorldRef world, RBlockPos origin, String sourceTypeKey, List<RBlockPos> affectedBlocks) {
        this(world, origin, sourceTypeKey, affectedBlocks, false);
    }

    public ExplosionPre(RWorldRef world, RBlockPos origin, String sourceTypeKey, List<RBlockPos> affectedBlocks, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.sourceTypeKey = Objects.requireNonNull(sourceTypeKey, "sourceTypeKey");
        this.affectedBlocks = Objects.requireNonNull(affectedBlocks, "affectedBlocks");
        this.cancelled = isCancelled;
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos origin() {
        return origin;
    }

    public String sourceTypeKey() {
        return sourceTypeKey;
    }

    /**
     * Mutable list of affected blocks. Listeners may remove entries to prevent them from being destroyed.
     */
    public List<RBlockPos> affectedBlocks() {
        return affectedBlocks;
    }
}
