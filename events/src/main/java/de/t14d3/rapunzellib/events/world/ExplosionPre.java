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

    /**
     * Creates a new ExplosionPre event.
     *
     * @param world          the world reference
     * @param origin         the origin position of the explosion
     * @param sourceTypeKey  the source type key (e.g., "creeper", "tnt")
     * @param affectedBlocks the list of blocks that will be affected
     */
    public ExplosionPre(RWorldRef world, RBlockPos origin, String sourceTypeKey, List<RBlockPos> affectedBlocks) {
        this(world, origin, sourceTypeKey, affectedBlocks, false);
    }

    /**
     * Creates a new ExplosionPre event with cancelled state.
     *
     * @param world          the world reference
     * @param origin         the origin position
     * @param sourceTypeKey  the source type key
     * @param affectedBlocks the list of affected blocks
     * @param isCancelled    whether the event is initially cancelled
     */
    public ExplosionPre(RWorldRef world, RBlockPos origin, String sourceTypeKey, List<RBlockPos> affectedBlocks, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.sourceTypeKey = Objects.requireNonNull(sourceTypeKey, "sourceTypeKey");
        this.affectedBlocks = Objects.requireNonNull(affectedBlocks, "affectedBlocks");
        setCancelled(isCancelled);
    }

    /**
     * Returns the world where the explosion occurs.
     *
     * @return the world
     */
    public RWorldRef world() {
        return world;
    }

    /**
     * Returns the origin position of the explosion.
     *
     * @return the origin position
     */
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
