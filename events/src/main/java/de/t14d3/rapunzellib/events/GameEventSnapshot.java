package de.t14d3.rapunzellib.events;

/**
 * Marker interface for immutable async-safe event payloads.
 *
 * <p>Live wrappers remain server-thread objects. Snapshot events should expose
 * shared snapshot value types such as block and entity snapshots whenever live
 * state must cross thread boundaries.</p>
 */
public non-sealed interface GameEventSnapshot extends GameEvent {
}
