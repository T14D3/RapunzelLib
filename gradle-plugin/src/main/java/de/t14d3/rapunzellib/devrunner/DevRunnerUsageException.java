package de.t14d3.rapunzellib.devrunner;

public final class DevRunnerUsageException extends RuntimeException {
    private final int code;

    public DevRunnerUsageException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
