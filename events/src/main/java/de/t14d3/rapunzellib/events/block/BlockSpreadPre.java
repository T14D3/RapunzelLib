package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Dispatched before a block spreads to another location (e.g., fire, mushrooms).
 */
public final class BlockSpreadPre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey newBlockTypeKey;
    private final RKey sourceBlockTypeKey;

    public BlockSpreadPre(RWorldRef world, RBlockPos pos, RKey newBlockTypeKey, RKey sourceBlockTypeKey) {
        this(world, pos, newBlockTypeKey, sourceBlockTypeKey, false);
    }

    public BlockSpreadPre(RWorldRef world, RBlockPos pos, String newBlockTypeKey, String sourceBlockTypeKey) {
        this(world, pos, RKey.of(newBlockTypeKey), RKey.of(sourceBlockTypeKey));
    }

    public BlockSpreadPre(RWorldRef world, RBlockPos pos, RKey newBlockTypeKey, RKey sourceBlockTypeKey, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.newBlockTypeKey = Objects.requireNonNull(newBlockTypeKey, "newBlockTypeKey");
        this.sourceBlockTypeKey = Objects.requireNonNull(sourceBlockTypeKey, "sourceBlockTypeKey");
        setCancelled(isCancelled);
    }

    public BlockSpreadPre(RWorldRef world, RBlockPos pos, String newBlockTypeKey, String sourceBlockTypeKey, boolean isCancelled) {
        this(world, pos, RKey.of(newBlockTypeKey), RKey.of(sourceBlockTypeKey), isCancelled);
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos pos() {
        return pos;
    }

    /**
     * The block type that will spread to this position.
     */
    public RKey newBlockTypeKey() {
        return newBlockTypeKey;
    }

    /**
     * The block type that is spreading (source of the spread).
     */
    public RKey sourceBlockTypeKey() {
        return sourceBlockTypeKey;
    }
}
