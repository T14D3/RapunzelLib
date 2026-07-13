package de.t14d3.rapunzellib.livetest;

public interface LiveTest {
    String name();
    void run() throws Exception;
    default void setupCommands() {}
    default long timeoutMs() { return 30_000L; }
}
