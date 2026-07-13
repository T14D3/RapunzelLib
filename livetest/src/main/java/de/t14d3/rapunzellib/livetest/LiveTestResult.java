package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The result of a single live test execution.
 * <p>
 * This is a value type used across the library, platform implementations,
 * and the Gradle plugin for reporting test outcomes.
 * </p>
 */
public final class LiveTestResult {

    /**
     * The status of a test execution.
     */
    public enum Status {
        PASS,
        FAIL,
        SKIP,
        ERROR
    }

    private final @NotNull String name;
    private final @NotNull Status status;
    private final long durationMs;
    private final @Nullable String message;

    private LiveTestResult(
            @NotNull String name,
            @NotNull Status status,
            long durationMs,
            @Nullable String message
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.status = Objects.requireNonNull(status, "status");
        this.durationMs = durationMs;
        this.message = message;
    }

    /**
     * Creates a passing result.
     *
     * @param name       the test name
     * @param durationMs the test duration in milliseconds
     * @return the result
     */
    public static @NotNull LiveTestResult pass(@NotNull String name, long durationMs) {
        return new LiveTestResult(name, Status.PASS, durationMs, null);
    }

    /**
     * Creates a failing result.
     *
     * @param name       the test name
     * @param durationMs the test duration in milliseconds
     * @param message    the failure message
     * @return the result
     */
    public static @NotNull LiveTestResult fail(@NotNull String name, long durationMs, @Nullable String message) {
        return new LiveTestResult(name, Status.FAIL, durationMs, message);
    }

    /**
     * Creates a skipped result.
     *
     * @param name    the test name
     * @param message the skip reason
     * @return the result
     */
    public static @NotNull LiveTestResult skip(@NotNull String name, @Nullable String message) {
        return new LiveTestResult(name, Status.SKIP, 0, message);
    }

    /**
     * Creates an error result.
     *
     * @param name       the test name
     * @param durationMs the test duration in milliseconds
     * @param message    the error message
     * @return the result
     */
    public static @NotNull LiveTestResult error(@NotNull String name, long durationMs, @Nullable String message) {
        return new LiveTestResult(name, Status.ERROR, durationMs, message);
    }

    /**
     * Returns the test name.
     *
     * @return the test name
     */
    public @NotNull String name() {
        return name;
    }

    /**
     * Returns the test status.
     *
     * @return the status
     */
    public @NotNull Status status() {
        return status;
    }

    /**
     * Returns the test duration in milliseconds.
     *
     * @return the duration
     */
    public long durationMs() {
        return durationMs;
    }

    /**
     * Returns the result message (failure reason, skip reason, or error details).
     *
     * @return the message, may be null
     */
    public @Nullable String message() {
        return message;
    }

    /**
     * Returns whether this result indicates a passed test.
     *
     * @return true if passed
     */
    public boolean passed() {
        return status == Status.PASS;
    }

    /**
     * Returns whether this result indicates a failed test.
     *
     * @return true if failed
     */
    public boolean failed() {
        return status == Status.FAIL;
    }

    /**
     * Returns whether this result indicates a skipped test.
     *
     * @return true if skipped
     */
    public boolean skipped() {
        return status == Status.SKIP;
    }

    /**
     * Returns whether this result indicates an error.
     *
     * @return true if error
     */
    public boolean isError() {
        return status == Status.ERROR;
    }

    /**
     * Formats this result as a {@code [LIVETEST]} string for console or log output.
     *
     * @return the formatted result string
     */
    public @NotNull String format() {
        String statusStr = switch (status) {
            case PASS -> "PASS";
            case FAIL -> "FAIL";
            case SKIP -> "SKIP";
            case ERROR -> "ERROR";
        };
        if (status == Status.PASS || status == Status.SKIP) {
            String suffix = message != null ? " (" + message + ")" : " (" + durationMs + "ms)";
            return "[LIVETEST] " + statusStr + " " + name + suffix;
        }
        String payload = message != null ? message : String.valueOf(durationMs);
        return "[LIVETEST] " + statusStr + " " + name + " (" + payload + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LiveTestResult that)) return false;
        return durationMs == that.durationMs
                && name.equals(that.name)
                && status == that.status
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, status, durationMs, message);
    }

    @Override
    public String toString() {
        return "LiveTestResult{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", durationMs=" + durationMs +
                ", message='" + message + '\'' +
                '}';
    }
}
