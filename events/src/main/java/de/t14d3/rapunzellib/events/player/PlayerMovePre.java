package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;

import java.util.Objects;

/**
 * Pre-event fired when a player moves from one location to another.
 *
 * <p>Dispatch is throttled by the library-scope {@code events.player.move}
 * config section (see {@link PlayerMoveThrottle}): at most one event per
 * player per {@code max-rate-ms}, and only when the player has displaced at
 * least {@code min-distance} since the last dispatched event. Set
 * {@code enabled: false} to disable the event entirely. The same throttle
 * applies on every platform (Paper, Fabric, NeoForge, Sponge).</p>
 *
 * <p>This event is cancellable, allowing plugins to deny player movement.
 * On Paper and Sponge the platform cancellation reverts the move; on
 * Fabric/NeoForge the mixin cancels the underlying {@code Entity.move} and
 * teleports the player back to {@link #from()} with client sync, so the
 * denial is effective (the player cannot keep walking).</p>
 */
public final class PlayerMovePre extends BaseCancellablePreEvent {
    private final RPlayer player;
    private final RLocation from;
    private final RLocation to;

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

    public RPlayer player() {
        return player;
    }

    public RLocation from() {
        return from;
    }

    public RLocation to() {
        return to;
    }
}
