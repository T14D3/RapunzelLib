package de.t14d3.rapunzellib.events.interact;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.block.RBlock;

import java.util.Objects;

public final class UseBlockPre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RBlock block;

    public UseBlockPre(RPlayer player, RBlock block) {
        this(player, block, false);
    }

    public UseBlockPre(RPlayer player, RBlock block, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.cancelled = isCancelled;
    }

    public RPlayer player() {
        return player;
    }

    public RBlock block() {
        return block;
    }
}
