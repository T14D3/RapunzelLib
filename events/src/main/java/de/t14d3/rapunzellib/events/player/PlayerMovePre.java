package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;

import java.util.Objects;

/**
 * Pre-event fired when a player moves from one location to another.
 * <p>This event is cancellable, allowing plugins to deny player movement.</p>
 */
public final class PlayerMovePre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RLocation from;
    private final RLocation to;

    /**
     * Creates a new PlayerMovePre event.
     * @param player the player who is moving
     * @param from the location the player is moving from
     * @param to the location the player is moving to
     */
    public PlayerMovePre(RPlayer player, RLocation from, RLocation to) {
        this(player, from, to, false);
    }

    /**
     * Creates a new PlayerMovePre event with optional cancelled state.
     * @param player the player who is moving
     * @param from the location the player is moving from
     * @param to the location the player is moving to
     * @param isCancelled whether the event is initially cancelled
     */
    public PlayerMovePre(RPlayer player, RLocation from, RLocation to, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        setCancelled(isCancelled);
    }

    /**
     * Returns the player who is moving.
     * @return the player
     */
    public RPlayer player() {
        return player;
    }

    /**
     * Returns the location the player is moving from.
     * @return the from location
     */
    public RLocation from() {
        return from;
    }

    /**
     * Returns the location the player is moving to.
     * @return the to location
     */
    public RLocation to() {
        return to;
    }
}
