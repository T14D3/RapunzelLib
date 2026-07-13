package de.t14d3.rapunzellib.events;

/**
 * Bridge for subscribing to and unsubscribing from game events.
 */
public interface GameEventBridge extends AutoCloseable {
    @Override
    void close();
}
