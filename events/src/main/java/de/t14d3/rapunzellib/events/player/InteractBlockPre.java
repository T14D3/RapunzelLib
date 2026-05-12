package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.block.RBlock;

import java.util.Objects;

/**
 * Pre-event fired before a player interacts with a block (left-click or right-click).
 *
 * <p>This event is cancellable. If denied, the interaction will not occur.
 * Contains the action type and which hand was used.</p>
 */
public final class InteractBlockPre extends BaseCancellablePreEvent {
    /**
     * The type of interaction action.
     */
    public enum Action {
        LEFT_CLICK_BLOCK,
        RIGHT_CLICK_BLOCK,
    }

    /**
     * The hand used for the interaction.
     */
    public enum Hand {
        MAIN_HAND,
        OFF_HAND,
        UNKNOWN,
    }

    private final RPlayer player;
    private final RBlock block;
    private final Action action;
    private final Hand hand;

    /**
     * Creates a new InteractBlockPre event.
     *
     * @param player the interacting player
     * @param block  the block being interacted with
     * @param action the action type
     * @param hand   the hand used
     */
    public InteractBlockPre(RPlayer player, RBlock block, Action action, Hand hand) {
        this(player, block, action, hand, false);
    }

    /**
     * Creates a new InteractBlockPre event with cancelled state.
     *
     * @param player      the interacting player
     * @param block       the block being interacted with
     * @param action      the action type
     * @param hand        the hand used
     * @param isCancelled whether the event is initially cancelled
     */
    public InteractBlockPre(RPlayer player, RBlock block, Action action, Hand hand, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.action = Objects.requireNonNull(action, "action");
        this.hand = Objects.requireNonNull(hand, "hand");
        setCancelled(isCancelled);
    }

    /**
     * Returns the interacting player.
     *
     * @return the player
     */
    public RPlayer player() {
        return player;
    }

    /**
     * Returns the block being interacted with.
     *
     * @return the block
     */
    public RBlock block() {
        return block;
    }

    /**
     * Returns the action type.
     *
     * @return the action
     */
    public Action action() {
        return action;
    }

    /**
     * Returns the hand used for the interaction.
     *
     * @return the hand
     */
    public Hand hand() {
        return hand;
    }
}
