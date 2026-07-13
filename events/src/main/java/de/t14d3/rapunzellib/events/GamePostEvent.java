package de.t14d3.rapunzellib.events;

/**
 * Marker interface for post-events that are fired after an action occurs.
 *
 * <p>Post-events are informational and allow handlers to react to completed
 * actions. They cannot be cancelled or modified as the action has already
 * been processed by the game.</p>
 */
public non-sealed interface GamePostEvent extends GameEvent {
}
