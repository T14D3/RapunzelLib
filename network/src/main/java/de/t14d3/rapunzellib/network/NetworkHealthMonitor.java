package de.t14d3.rapunzellib.network;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks network transport health for observability.
 */
public final class NetworkHealthMonitor {
    public record ConnectionHealth(
        String transport,
        boolean connected,
        long latencyMs,
        long lastSuccess,
        int failuresLastMinute
    ) {
    }

    private static final long FAILURE_WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, ConnectionHealth> healthMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FailureWindow> failureWindows = new ConcurrentHashMap<>();

    public ConnectionHealth getHealth(String transport) {
        return healthMap.get(transport);
    }

    public void recordSuccess(String transport, long latencyMs) {
        String key = normalize(transport);
        long now = System.currentTimeMillis();
        FailureWindow window = failureWindows.get(key);
        int failures = window != null ? window.failures : 0;
        healthMap.put(
            key,
            new ConnectionHealth(key, true, latencyMs, now, failures)
        );
    }

    public void recordFailure(String transport) {
        String key = normalize(transport);
        long now = System.currentTimeMillis();
        FailureWindow window = failureWindows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart > FAILURE_WINDOW_MS) {
                return new FailureWindow(now, 1);
            }
            existing.failures++;
            return existing;
        });

        ConnectionHealth previous = healthMap.get(key);
        long lastSuccess = previous != null ? previous.lastSuccess() : 0L;
        int failures = window != null ? window.failures : 1;
        healthMap.put(
            key,
            new ConnectionHealth(key, false, -1L, lastSuccess, failures)
        );
    }

    private static String normalize(String transport) {
        if (transport == null) {
            return "unknown";
        }
        String trimmed = transport.trim();
        return trimmed.isBlank() ? "unknown" : trimmed;
    }

    private static final class FailureWindow {
        private final long windowStart;
        private int failures;

        private FailureWindow(long windowStart, int failures) {
            this.windowStart = windowStart;
            this.failures = failures;
        }
    }
}
