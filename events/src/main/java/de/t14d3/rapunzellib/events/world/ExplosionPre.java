package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.List;
import java.util.Objects;

/**
 * Pre-event fired before an explosion occurs.
 *
 * <p>This event is cancellable. If denied, the explosion will not happen.
 * The affected blocks list is mutable, allowing listeners to protect specific blocks.</p>
 */
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
        setCancelled(isCancelled);
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos origin() {
        return origin;
    }

    /**
     * Returns the source type key (e.g., "creeper", "tnt", "fireball").
     *
     * @return the source type key
     */
    public String sourceTypeKey() {
        return sourceTypeKey;
    }

    /**
     * Returns a mutable list of blocks that will be affected by the explosion.
     * Listeners may remove entries from this list to prevent specific blocks from being destroyed.
     *
     * @return the list of affected block positions
     */
    public List<RBlockPos> affectedBlocks() {
        return affectedBlocks;
    }
}
