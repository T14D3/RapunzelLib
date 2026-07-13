package de.t14d3.rapunzellib.livetest;

public class LiveTestSkipException extends RuntimeException {
    public LiveTestSkipException(String message) { super(message); }
    public LiveTestSkipException(String message, Throwable cause) { super(message, cause); }
}
