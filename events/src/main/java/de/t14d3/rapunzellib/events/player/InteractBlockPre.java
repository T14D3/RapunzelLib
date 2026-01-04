package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.block.RBlock;

import java.util.Objects;

public final class InteractBlockPre extends BaseCancellablePreEvent {
    public enum Action {
        LEFT_CLICK_BLOCK,
        RIGHT_CLICK_BLOCK,
    }

    public enum Hand {
        MAIN_HAND,
        OFF_HAND,
        UNKNOWN,
    }

    private final RPlayer player;
    private final RBlock block;
    private final Action action;
    private final Hand hand;

    public InteractBlockPre(RPlayer player, RBlock block, Action action, Hand hand) {
        this(player, block, action, hand, false);
    }

    public InteractBlockPre(RPlayer player, RBlock block, Action action, Hand hand, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.action = Objects.requireNonNull(action, "action");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.cancelled = isCancelled;
    }

    public RPlayer player() {
        return player;
    }

    public RBlock block() {
        return block;
    }

    public Action action() {
        return action;
    }

    public Hand hand() {
        return hand;
    }
}
