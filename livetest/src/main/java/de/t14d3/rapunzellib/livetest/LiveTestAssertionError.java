package de.t14d3.rapunzellib.livetest;

public class LiveTestAssertionError extends RuntimeException {
    public LiveTestAssertionError(String message) { super(message); }
    public LiveTestAssertionError(String message, Throwable cause) { super(message, cause); }
}
