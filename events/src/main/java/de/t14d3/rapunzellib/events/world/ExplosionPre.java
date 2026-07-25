package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RLocation;
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
    private final RLocation origin;
    private final RKey sourceTypeKey;
    private final ExplosionSourceKind sourceKind;
    private final List<RBlockPos> affectedBlocks;


    public ExplosionPre(RLocation origin, RKey sourceTypeKey, ExplosionSourceKind sourceKind, List<RBlockPos> affectedBlocks, boolean isCancelled) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.sourceTypeKey = Objects.requireNonNull(sourceTypeKey, "sourceTypeKey");
        this.sourceKind = Objects.requireNonNull(sourceKind, "sourceKind");
        this.affectedBlocks = Objects.requireNonNull(affectedBlocks, "affectedBlocks");
        setCancelled(isCancelled);
    }

    public ExplosionPre(RLocation origin, String sourceTypeKey, ExplosionSourceKind sourceKind, List<RBlockPos> affectedBlocks, boolean isCancelled) {
        this(origin, RKey.of(sourceTypeKey), sourceKind, affectedBlocks, isCancelled);
    }

    public RLocation origin() {
        return origin;
    }

    /**
     * Returns the source type key identifying the cause of the explosion.
     *
     * <p>Depending on {@link #sourceKind()}, the returned key belongs to either
     * the entity-type registry ({@code ExplosionSourceKind.ENTITY}) or the
     * block-type registry ({@code ExplosionSourceKind.BLOCK}). For
     * {@code OTHER}, the key is plugin-defined and may not resolve in either
     * registry.</p>
     *
     * @return the source type key
     */
    public RKey sourceTypeKey() {
        return sourceTypeKey;
    }

    /**
     * Returns the kind of object that caused the explosion.
     *
     * <p>Consumers pair this with {@link #sourceTypeKey()} to dereference the
     * correct registry: {@code REntityType.find(sourceTypeKey())} when
     * {@code sourceKind() == ENTITY}, or {@code RBlockType.find(sourceTypeKey())}
     * when {@code sourceKind() == BLOCK}.</p>
     *
     * @return the source kind
     */
    public ExplosionSourceKind sourceKind() {
        return sourceKind;
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
