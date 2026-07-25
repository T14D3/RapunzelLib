package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.registry.RBlockType;

import java.util.Objects;
import java.util.Optional;

/**
 * Pre-event fired before TNT is primed (ignited).
 *
 * <p>This event is cancellable. If denied, the TNT will not be primed.</p>
 */
public final class TntPrimePre extends BaseCancellablePreEvent {
    private final RBlock block;
    private final String cause;
    private final RPlayer player;

    public TntPrimePre(RBlock block, String cause, RPlayer player) {
        this(block, cause, player, false);
    }

    public TntPrimePre(RBlock block, String cause, RPlayer player, boolean isCancelled) {
        this.block = Objects.requireNonNull(block, "block");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.player = player;
        setCancelled(isCancelled);
    }

    /**
     * Returns the live wrapper for the TNT block being primed.
     *
     * @return the live TNT block
     */
    public RBlock block() {
        return block;
    }

    /**
     * Returns the block type of the TNT being primed.
     *
     * @return the block type
     */
    public RBlockType blockType() {
        return block.requireType();
    }

    /**
     * Returns the key of the TNT block type.
     *
     * @return the block type key
     */
    public RKey blockTypeKey() {
        return block.typeKey();
    }

    /**
     * Returns the world reference of the TNT block.
     *
     * @return the world reference
     */
    public RWorldRef world() {
        return block.world().ref();
    }

    /**
     * Returns the position of the TNT block.
     *
     * @return the block position
     */
    public RBlockPos pos() {
        return block.pos();
    }

    /**
     * Returns the cause of priming (e.g., "flint_and_steel", "fire", "redstone").
     *
     * @return the cause
     */
    public String cause() {
        return cause;
    }

    public Optional<RPlayer> player() {
        return Optional.ofNullable(player);
    }
}
