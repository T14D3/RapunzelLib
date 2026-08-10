package de.t14d3.rapunzellib.events.player;

import de.t14d3.rapunzellib.events.GamePostEvent;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Post-event fired after a player has moved.
 *
 * <p>Fires only for moves whose pre-event passed the library-scope throttle
 * ({@code events.player.move} config section, see {@link PlayerMoveThrottle}),
 * so pre/post stay paired exactly.</p>
 *
 * <p>This event is immutable and contains information about the completed movement.
 * It can be used for logging, region entries/exits, and other post-movement processing.
 */
public record PlayerMovePost(RPlayer player, RLocation from, RLocation to, boolean isCancelled) implements GamePostEvent {

    public PlayerMovePost(RPlayer player, RLocation from, RLocation to, boolean isCancelled) {
        this.player = Objects.requireNonNull(player, "player");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.isCancelled = isCancelled;
    }
}
