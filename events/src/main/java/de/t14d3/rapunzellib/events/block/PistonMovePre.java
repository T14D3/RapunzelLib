package de.t14d3.rapunzellib.events.block;

import de.t14d3.rapunzellib.events.BaseCancellablePreEvent;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.List;
import java.util.Objects;

/**
 * Pre-event fired before a piston moves blocks (extension or retraction).
 *
 * <p>Cancellable. Carries the source position and the destination position of
 * every block the piston would move; consumers can compare the regions at each
 * pair to decide whether the move may cross a protected boundary. The
 * triggering player is not exposed because not every platform provides it.</p>
 */
public final class PistonMovePre extends BaseCancellablePreEvent {

    public enum Action { EXTEND, RETRACT }

    private final RWorldRef world;
    private final List<RBlockPos> sources;
    private final List<RBlockPos> destinations;
    private final Action action;

    public PistonMovePre(RWorldRef world, List<RBlockPos> sources, List<RBlockPos> destinations, Action action) {
        this(world, sources, destinations, action, false);
    }

    public PistonMovePre(RWorldRef world, List<RBlockPos> sources, List<RBlockPos> destinations,
                         Action action, boolean isCancelled) {
        this.world = Objects.requireNonNull(world, "world");
        this.sources = List.copyOf(sources);
        this.destinations = List.copyOf(destinations);
        if (this.sources.size() != this.destinations.size()) {
            throw new IllegalArgumentException("sources and destinations must have the same size");
        }
        this.action = Objects.requireNonNull(action, "action");
        setCancelled(isCancelled);
    }

    /** Returns the world the piston operates in. */
    public RWorldRef world() {
        return world;
    }

    /** Returns the current position of every block the piston would move. */
    public List<RBlockPos> sources() {
        return sources;
    }

    /** Returns the destination position of every moved block (same order as {@link #sources()}). */
    public List<RBlockPos> destinations() {
        return destinations;
    }

    /** Returns whether the piston is extending or retracting. */
    public Action action() {
        return action;
    }
}
