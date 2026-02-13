package de.t14d3.rapunzellib.serverrunner;

final class ServerRunnerUsageException extends RuntimeException {
    private final int code;

    ServerRunnerUsageException(int code, String message) {
        super(message);
        this.code = code;
    }

    int code() {
        return code;
    }
}
