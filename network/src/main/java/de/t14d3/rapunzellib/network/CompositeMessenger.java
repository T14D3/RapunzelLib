package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Messenger wrapper that attempts sends on a primary messenger and falls back to others.
 */
/**
 * A messenger that delegates to a primary messenger with configurable fallbacks.
 */
public final class CompositeMessenger implements Messenger {
    private final Messenger primary;
    private final List<Messenger> messengers;
    private final NetworkHealthMonitor healthMonitor;
    private final Logger logger;

    public CompositeMessenger(@NotNull Messenger primary, @NotNull List<Messenger> fallbacks) {
        this(primary, fallbacks, LoggerFactory.getLogger(CompositeMessenger.class), new NetworkHealthMonitor());
    }

    public CompositeMessenger(
        @NotNull Messenger primary,
        @NotNull List<Messenger> fallbacks,
        @NotNull Logger logger,
        @NotNull NetworkHealthMonitor healthMonitor
    ) {
        this.primary = Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(fallbacks, "fallbacks");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.healthMonitor = Objects.requireNonNull(healthMonitor, "healthMonitor");

        LinkedHashSet<Messenger> ordered = new LinkedHashSet<>();
        ordered.add(primary);
        ordered.addAll(fallbacks);
        this.messengers = Collections.unmodifiableList(new ArrayList<>(ordered));
    }

    public @NotNull NetworkHealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    @Override
    public void sendToAll(@NotNull String channel, @NotNull String data) {
        if (trySend(primary, () -> primary.sendToAll(channel, data))) {
            return;
        }
        for (Messenger messenger : messengers) {
            if (messenger == primary) continue;
            if (trySend(messenger, () -> messenger.sendToAll(channel, data))) {
                return;
            }
        }
        logger.error("All messengers failed to send to channel {}", channel);
    }

    @Override
    public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
        if (trySend(primary, () -> primary.sendToServer(channel, serverName, data))) {
            return;
        }
        for (Messenger messenger : messengers) {
            if (messenger == primary) continue;
            if (trySend(messenger, () -> messenger.sendToServer(channel, serverName, data))) {
                return;
            }
        }
        logger.error("All messengers failed to send to server {} on {}", serverName, channel);
    }

    @Override
    public void sendToProxy(@NotNull String channel, @NotNull String data) {
        if (trySend(primary, () -> primary.sendToProxy(channel, data))) {
            return;
        }
        for (Messenger messenger : messengers) {
            if (messenger == primary) continue;
            if (trySend(messenger, () -> messenger.sendToProxy(channel, data))) {
                return;
            }
        }
        logger.error("All messengers failed to send to proxy channel {}", channel);
    }

    @Override
    public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
        primary.registerListener(channel, listener);
    }

    @Override
    public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
        primary.unregisterListener(channel, listener);
    }

    @Override
    public boolean isConnected() {
        for (Messenger messenger : messengers) {
            if (messenger.isConnected()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull String getServerName() {
        return firstNonBlank(Messenger::getServerName);
    }

    @Override
    public @NotNull String getProxyServerName() {
        return firstNonBlank(Messenger::getProxyServerName);
    }

    private boolean trySend(Messenger messenger, Runnable send) {
        String transport = transportName(messenger);
        if (!messenger.isConnected()) {
            healthMonitor.recordFailure(transport);
            return false;
        }

        long start = System.nanoTime();
        try {
            send.run();
            long latencyMs = (System.nanoTime() - start) / 1_000_000L;
            healthMonitor.recordSuccess(transport, latencyMs);
            return true;
        } catch (Exception e) {
            healthMonitor.recordFailure(transport);
            logger.warn("Messenger {} failed, trying fallback", transport, e);
            return false;
        }
    }

    private String transportName(Messenger messenger) {
        if (messenger == null) {
            return "unknown";
        }
        String simple = messenger.getClass().getSimpleName();
        return simple == null || simple.isBlank() ? messenger.getClass().getName() : simple;
    }

    private String firstNonBlank(NameSupplier supplier) {
        for (Messenger messenger : messengers) {
            String value = supplier.get(messenger);
            if (value == null) continue;
            String trimmed = value.trim();
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }
        return "unknown";
    }

    private interface NameSupplier {
        String get(Messenger messenger);
    }
}
