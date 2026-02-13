package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Dispatched before a block transforms (e.g., oxidation, waxing, etc.).
 */
public final class BlockTransformPre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey originalBlockTypeKey;
    private final RKey transformedBlockTypeKey;

    public BlockTransformPre(RWorldRef world, RBlockPos pos, RKey originalBlockTypeKey, RKey transformedBlockTypeKey) {
        this(world, pos, originalBlockTypeKey, transformedBlockTypeKey, false);
    }

    public BlockTransformPre(RWorldRef world, RBlockPos pos, String originalBlockTypeKey, String transformedBlockTypeKey) {
        this(world, pos, RKey.of(originalBlockTypeKey), RKey.of(transformedBlockTypeKey));
    }

    public BlockTransformPre(RWorldRef world, RBlockPos pos, RKey originalBlockTypeKey, RKey transformedBlockTypeKey, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.originalBlockTypeKey = Objects.requireNonNull(originalBlockTypeKey, "originalBlockTypeKey");
        this.transformedBlockTypeKey = Objects.requireNonNull(transformedBlockTypeKey, "transformedBlockTypeKey");
        setCancelled(isCancelled);
    }

    public BlockTransformPre(RWorldRef world, RBlockPos pos, String originalBlockTypeKey, String transformedBlockTypeKey, boolean isCancelled) {
        this(world, pos, RKey.of(originalBlockTypeKey), RKey.of(transformedBlockTypeKey), isCancelled);
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos pos() {
        return pos;
    }

    /**
     * The block type before transformation.
     */
    public RKey originalBlockTypeKey() {
        return originalBlockTypeKey;
    }

    /**
     * The block type after transformation.
     */
    public RKey transformedBlockTypeKey() {
        return transformedBlockTypeKey;
    }
}
