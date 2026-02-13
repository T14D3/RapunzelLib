package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

/**
 * Dispatched before a block forms naturally (e.g., ice, snow, concrete powder).
 */
public final class BlockFormPre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final RKey newBlockTypeKey;
    private final RKey sourceBlockTypeKey;

    public BlockFormPre(RWorldRef world, RBlockPos pos, RKey newBlockTypeKey, RKey sourceBlockTypeKey) {
        this(world, pos, newBlockTypeKey, sourceBlockTypeKey, false);
    }

    public BlockFormPre(RWorldRef world, RBlockPos pos, String newBlockTypeKey, String sourceBlockTypeKey) {
        this(world, pos, RKey.of(newBlockTypeKey), RKey.of(sourceBlockTypeKey));
    }

    public BlockFormPre(RWorldRef world, RBlockPos pos, RKey newBlockTypeKey, RKey sourceBlockTypeKey, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.newBlockTypeKey = Objects.requireNonNull(newBlockTypeKey, "newBlockTypeKey");
        this.sourceBlockTypeKey = Objects.requireNonNull(sourceBlockTypeKey, "sourceBlockTypeKey");
        setCancelled(isCancelled);
    }

    public BlockFormPre(RWorldRef world, RBlockPos pos, String newBlockTypeKey, String sourceBlockTypeKey, boolean isCancelled) {
        this(world, pos, RKey.of(newBlockTypeKey), RKey.of(sourceBlockTypeKey), isCancelled);
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos pos() {
        return pos;
    }

    /**
     * The block type that will be formed.
     */
    public RKey newBlockTypeKey() {
        return newBlockTypeKey;
    }

    /**
     * The block type that is the source of the formation (e.g., water for ice).
     */
    public RKey sourceBlockTypeKey() {
        return sourceBlockTypeKey;
    }
}
