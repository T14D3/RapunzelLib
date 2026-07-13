package de.t14d3.rapunzellib.events;

/**
 * Marker interface for pre-events that are fired before an action occurs.
 *
 * <p>Pre-events allow handlers to inspect, modify, or cancel upcoming actions
 * before they are processed by the game. Implementations may extend
 * {@link CancellablePreEvent} to support cancellation and decision-making.</p>
 */
public non-sealed interface GamePreEvent extends GameEvent {
}
