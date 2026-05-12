package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;
import java.util.Optional;

/**
 * Pre-event fired before TNT is primed (ignited).
 *
 * <p>This event is cancellable. If denied, the TNT will not be primed.</p>
 */
public final class TntPrimePre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey blockTypeKey;
    private final String cause;
    private final RPlayer player;

    /**
     * Creates a new TntPrimePre event.
     *
     * @param world         the world reference
     * @param pos           the position of the TNT
     * @param blockTypeKey  the block type key
     * @param cause         the cause of priming
     * @param player        the player who primed the TNT, may be null
     */
    public TntPrimePre(RWorldRef world, RBlockPos pos, RKey blockTypeKey, String cause, RPlayer player) {
        this(world, pos, blockTypeKey, cause, player, false);
    }

    /**
     * Creates a new TntPrimePre event with string block type key.
     *
     * @param world         the world reference
     * @param pos           the position of the TNT
     * @param blockTypeKey  the block type key as a string
     * @param cause         the cause of priming
     * @param player        the player who primed the TNT, may be null
     */
    public TntPrimePre(RWorldRef world, RBlockPos pos, String blockTypeKey, String cause, RPlayer player) {
        this(world, pos, RKey.of(blockTypeKey), cause, player);
    }

    /**
     * Creates a new TntPrimePre event with cancelled state.
     *
     * @param world         the world reference
     * @param pos           the position
     * @param blockTypeKey  the block type key
     * @param cause         the cause of priming
     * @param player        the player who primed the TNT, may be null
     * @param isCancelled   whether the event is initially cancelled
     */
    public TntPrimePre(RWorldRef world, RBlockPos pos, RKey blockTypeKey, String cause, RPlayer player, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.blockTypeKey = Objects.requireNonNull(blockTypeKey, "blockTypeKey");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.player = player;
        setCancelled(isCancelled);
    }

    /**
     * Creates a new TntPrimePre event with cancelled state and string key.
     *
     * @param world         the world reference
     * @param pos           the position
     * @param blockTypeKey  the block type key as a string
     * @param cause         the cause of priming
     * @param player        the player who primed the TNT, may be null
     * @param isCancelled   whether the event is initially cancelled
     */
    public TntPrimePre(RWorldRef world, RBlockPos pos, String blockTypeKey, String cause, RPlayer player, boolean isCancelled) {
        this(world, pos, RKey.of(blockTypeKey), cause, player, isCancelled);
    }

    /**
     * Returns the world where the TNT is located.
     *
     * @return the world
     */
    public RWorldRef world() {
        return world;
    }

    /**
     * Returns the position of the TNT.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return pos;
    }

    /**
     * Returns the block type key of the TNT.
     *
     * @return the block type key
     */
    public RKey blockTypeKey() {
        return blockTypeKey;
    }

    /**
     * Returns the cause of priming (e.g., "flint_and_steel", "fire", "redstone").
     *
     * @return the cause
     */
    public String cause() {
        return cause;
    }

    /**
     * Returns the player who primed the TNT, if any.
     *
     * @return an optional containing the player
     */
    public Optional<RPlayer> player() {
        return Optional.ofNullable(player);
    }
}
