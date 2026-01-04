package de.t14d3.rapunzellib.events;

public interface GameEventBridge extends AutoCloseable {
    @Override
    void close();
}

