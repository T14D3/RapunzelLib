package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Pre-event fired before a block is placed by a player.
 *
 * <p>This event is cancellable. If denied, the block will not be placed.
 * Contains information about the player, world, position, and block type.</p>
 */
public final class BlockPlacePre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey blockTypeKey;

    /**
     * Creates a new BlockPlacePre event.
     *
     * @param player      the player placing the block
     * @param world       the world reference
     * @param pos         the block position
     * @param blockTypeKey the block type key
     */
    public BlockPlacePre(RPlayer player, RWorldRef world, RBlockPos pos, RKey blockTypeKey) {
        this(player, world, pos, blockTypeKey, false);
    }

    /**
     * Creates a new BlockPlacePre event with a string block type key.
     *
     * @param player      the player placing the block
     * @param world       the world reference
     * @param pos         the block position
     * @param blockTypeKey the block type key as a string
     */
    public BlockPlacePre(RPlayer player, RWorldRef world, RBlockPos pos, String blockTypeKey) {
        this(player, world, pos, RKey.of(blockTypeKey));
    }

    /**
     * Creates a new BlockPlacePre event with cancelled state.
     *
     * @param player      the player placing the block
     * @param world       the world reference
     * @param pos         the block position
     * @param blockTypeKey the block type key
     * @param isCancelled whether the event is initially cancelled
     */
    public BlockPlacePre(RPlayer player, RWorldRef world, RBlockPos pos, RKey blockTypeKey, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.blockTypeKey = Objects.requireNonNull(blockTypeKey, "blockTypeKey");
        setCancelled(isCancelled);
    }

    /**
     * Creates a new BlockPlacePre event with cancelled state and string key.
     *
     * @param player      the player placing the block
     * @param world       the world reference
     * @param pos         the block position
     * @param blockTypeKey the block type key as a string
     * @param isCancelled whether the event is initially cancelled
     */
    public BlockPlacePre(RPlayer player, RWorldRef world, RBlockPos pos, String blockTypeKey, boolean isCancelled) {
        this(player, world, pos, RKey.of(blockTypeKey), isCancelled);
    }

    /**
     * Returns the player placing the block.
     *
     * @return the player
     */
    public RPlayer player() {
        return player;
    }

    /**
     * Returns the world where the block is being placed.
     *
     * @return the world
     */
    public RWorldRef world() {
        return world;
    }

    /**
     * Returns the position where the block is being placed.
     *
     * @return the position
     */
    public RBlockPos pos() {
        return pos;
    }

    /**
     * Returns the block type key of the block being placed.
     *
     * @return the block type key
     */
    public RKey blockTypeKey() {
        return blockTypeKey;
    }
}
