package de.t14d3.rapunzellib.events.world;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;
import java.util.Optional;

public final class TntPrimePre extends BaseCancellablePreEvent {
    private final RWorldRef world;
    private final RBlockPos pos;
    private final String blockTypeKey;
    private final String cause;
    private final RPlayer player;

    public TntPrimePre(RWorldRef world, RBlockPos pos, String blockTypeKey, String cause, RPlayer player) {
        this(world, pos, blockTypeKey, cause, player, false);
    }

    public TntPrimePre(RWorldRef world, RBlockPos pos, String blockTypeKey, String cause, RPlayer player, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.blockTypeKey = Objects.requireNonNull(blockTypeKey, "blockTypeKey");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.player = player;
        this.cancelled = isCancelled;
    }

    public RWorldRef world() {
        return world;
    }

    public RBlockPos pos() {
        return pos;
    }

    public String blockTypeKey() {
        return blockTypeKey;
    }

    public String cause() {
        return cause;
    }

    public Optional<RPlayer> player() {
        return Optional.ofNullable(player);
    }
}
