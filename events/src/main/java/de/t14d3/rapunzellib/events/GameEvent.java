package de.t14d3.rapunzellib.events;

/**
 * Base interface for all game events in the RapunzelLib event system.
 *
 * <p>This sealed interface permits three types of events:
 * <ul>
 *   <li>{@link GamePreEvent} - Events fired before an action occurs</li>
 *   <li>{@link GamePostEvent} - Events fired after an action occurs</li>
 *   <li>{@link GameEventSnapshot} - Immutable snapshot events for async processing</li>
 * </ul>
 */
public sealed interface GameEvent permits GamePreEvent, GamePostEvent, GameEventSnapshot {
}

