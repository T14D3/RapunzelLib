package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Simple DB-backed outbox wrapper for {@link Messenger}.
 *
 * <p>When immediate delivery isn't possible, selected channels are persisted to a shared DB and retried
 * periodically.</p>
 */
public final class DbQueuedMessenger implements Messenger, AutoCloseable {
    private final SpoolDatabase database;
    private final NetworkOutboxRepository repository;
    private final Messenger delegate;
    private final Logger logger;
    private final String ownerId;
    private final Set<String> channelAllowlist;
    private final int maxBatchSize;
    private final long maxAgeMillis;
    private final Supplier<List<String>> allServersSupplier;
    private final Predicate<String> canSendToServerOverride;

    private final AtomicBoolean flushing = new AtomicBoolean();
    private final ScheduledTask flushTask;

    public DbQueuedMessenger(
        SpoolDatabase database,
        Messenger delegate,
        Scheduler scheduler,
        Logger logger,
        String ownerId,
        Set<String> channelAllowlist,
        Duration flushPeriod,
        int maxBatchSize,
        Duration maxAge
    ) {
        this(
            database,
            delegate,
            scheduler,
            logger,
            ownerId,
            channelAllowlist,
            flushPeriod,
            maxBatchSize,
            maxAge,
            null,
            null
        );
    }

    public DbQueuedMessenger(
        SpoolDatabase database,
        Messenger delegate,
        Scheduler scheduler,
        Logger logger,
        String ownerId,
        Set<String> channelAllowlist,
        Duration flushPeriod,
        int maxBatchSize,
        Duration maxAge,
        Supplier<List<String>> allServersSupplier,
        Predicate<String> canSendToServerOverride
    ) {
        this.database = Objects.requireNonNull(database, "database");
        this.repository = new NetworkOutboxRepository(database.entityManager());

        this.delegate = Objects.requireNonNull(delegate, "delegate");
        Objects.requireNonNull(scheduler, "scheduler");
        this.logger = Objects.requireNonNull(logger, "logger");

        String normalizedOwner = (ownerId == null) ? "" : ownerId.trim();
        if (normalizedOwner.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        this.ownerId = normalizedOwner;

        this.channelAllowlist = normalizeAllowlist(channelAllowlist);
        this.maxBatchSize = Math.max(0, maxBatchSize);
        this.maxAgeMillis = (maxAge == null) ? 0L : Math.max(0L, maxAge.toMillis());

        this.allServersSupplier = allServersSupplier;
        this.canSendToServerOverride = canSendToServerOverride;

        Duration period = (flushPeriod == null) ? Duration.ofSeconds(2) : flushPeriod;
        if (period.isNegative() || period.isZero()) {
            period = Duration.ofSeconds(2);
        }
        this.flushTask = scheduler.runRepeating(Duration.ofSeconds(1), period, this::flush);
    }

    @Override
    public void sendToAll(String channel, String data) {
        if (shouldQueue(channel)) {
            List<String> servers = allServers();
            if (!servers.isEmpty()) {
                for (String serverName : servers) {
                    if (serverName == null || serverName.isBlank()) continue;
                    sendToServer(channel, serverName, data);
                }
                return;
            }
        }

        if (canSendToAll()) {
            delegate.sendToAll(channel, data);
            return;
        }
        if (shouldQueue(channel)) {
            enqueue(NetworkEnvelope.Target.ALL, null, channel, data);
            return;
        }
        delegate.sendToAll(channel, data);
    }

    @Override
    public void sendToServer(String channel, String serverName, String data) {
        if (canSendToServer(serverName)) {
            delegate.sendToServer(channel, serverName, data);
            return;
        }
        if (shouldQueue(channel)) {
            enqueue(NetworkEnvelope.Target.SERVER, serverName, channel, data);
            return;
        }
        delegate.sendToServer(channel, serverName, data);
    }

    @Override
    public void sendToProxy(String channel, String data) {
        if (canSendToProxy()) {
            delegate.sendToProxy(channel, data);
            return;
        }
        if (shouldQueue(channel)) {
            enqueue(NetworkEnvelope.Target.PROXY, null, channel, data);
            return;
        }
        delegate.sendToProxy(channel, data);
    }

    @Override
    public void registerListener(String channel, MessageListener listener) {
        delegate.registerListener(channel, listener);
    }

    @Override
    public void unregisterListener(String channel, MessageListener listener) {
        delegate.unregisterListener(channel, listener);
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public String getServerName() {
        return delegate.getServerName();
    }

    @Override
    public String getProxyServerName() {
        return delegate.getProxyServerName();
    }

    @Override
    public void close() {
        try {
            flushTask.cancel();
        } catch (Exception ignored) {
        }
    }

    private boolean canSendToAll() {
        return delegate.isConnected();
    }

    private boolean canSendToServer(String serverName) {
        if (serverName == null || serverName.isBlank()) return false;
        String normalized = serverName.trim();

        Predicate<String> override = canSendToServerOverride;
        if (override != null) {
            try {
                return override.test(normalized);
            } catch (Exception ignored) {
                return false;
            }
        }

        return delegate.isConnected();
    }

    private boolean canSendToProxy() {
        return delegate.isConnected();
    }

    private boolean shouldQueue(String channel) {
        if (channel == null) return false;
        if (channelAllowlist.isEmpty()) return false;
        return channelAllowlist.contains(channel.trim());
    }

    private List<String> allServers() {
        Supplier<List<String>> supplier = allServersSupplier;
        if (supplier == null) return List.of();
        try {
            List<String> servers = supplier.get();
            if (servers == null || servers.isEmpty()) return List.of();
            return List.copyOf(servers);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void enqueue(NetworkEnvelope.Target target, String targetServer, String channel, String data) {
        if (target == null) return;
        if (channel == null || channel.isBlank()) return;
        final String payload = (data == null) ? "" : data;
        final String normalizedChannel = channel.trim();

        final long now = System.currentTimeMillis();
        final String normalizedTargetServer = (targetServer == null) ? null : targetServer.trim();

        database.runLocked(() -> {
            NetworkOutboxMessage msg = new NetworkOutboxMessage();
            msg.setOwnerId(ownerId);
            msg.setChannel(normalizedChannel);
            msg.setData(payload);
            msg.setTarget(target.name());
            msg.setTargetServer((normalizedTargetServer == null || normalizedTargetServer.isBlank()) ? null : normalizedTargetServer);
            msg.setCreatedAt(now);
            msg.setAttempts(0);
            msg.setLastAttemptAt(0L);

            repository.save(msg);
            database.entityManager().flush();
        });
    }

    private void flush() {
        if (channelAllowlist.isEmpty()) return;
        if (maxBatchSize <= 0) return;
        if (!flushing.compareAndSet(false, true)) return;

        try {
            flushOnce();
        } catch (Exception e) {
            logger.debug("Outbox flush failed: {}", e.getMessage());
        } finally {
            flushing.set(false);
        }
    }

    private void flushOnce() {
        List<NetworkOutboxMessage> batch = database.locked(() -> repository
            .findBy("ownerId", ownerId)
            .stream()
            .sorted(Comparator.comparingLong(NetworkOutboxMessage::getId))
            .limit(maxBatchSize)
            .toList()
        );

        if (batch.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<Long> deleteIds = new ArrayList<>();

        for (NetworkOutboxMessage msg : batch) {
            if (msg == null) continue;

            if (maxAgeMillis > 0L && msg.getCreatedAt() > 0L && (now - msg.getCreatedAt()) > maxAgeMillis) {
                deleteIds.add(msg.getId());
                continue;
            }

            String channel = msg.getChannel();
            if (!shouldQueue(channel)) {
                deleteIds.add(msg.getId());
                continue;
            }

            NetworkEnvelope.Target target = parseTarget(msg.getTarget());
            if (target == null) {
                deleteIds.add(msg.getId());
                continue;
            }

            if (target == NetworkEnvelope.Target.SERVER
                && (msg.getTargetServer() == null || msg.getTargetServer().isBlank())) {
                deleteIds.add(msg.getId());
                continue;
            }

            if (!canSend(target, msg.getTargetServer())) {
                continue;
            }

            try {
                deliver(target, msg.getTargetServer(), channel, msg.getData());
                deleteIds.add(msg.getId());
            } catch (Exception e) {
                recordAttempt(msg.getId(), now);
            }
        }

        if (!deleteIds.isEmpty()) {
            database.runLocked(() -> {
                for (Long id : deleteIds) {
                    if (id == null) continue;
                    repository.deleteById(id);
                }
            });
            database.flushAsync();
        }
    }

    private boolean canSend(NetworkEnvelope.Target target, String targetServer) {
        return switch (target) {
            case ALL -> canSendToAll();
            case PROXY -> canSendToProxy();
            case SERVER -> canSendToServer(targetServer);
        };
    }

    private void deliver(NetworkEnvelope.Target target, String targetServer, String channel, String data) {
        switch (target) {
            case ALL -> delegate.sendToAll(channel, data);
            case PROXY -> delegate.sendToProxy(channel, data);
            case SERVER -> delegate.sendToServer(channel, targetServer, data);
        }
    }

    private void recordAttempt(long id, long now) {
        if (id <= 0L) return;
        database.runLocked(() -> {
            NetworkOutboxMessage existing = repository.findById(id);
            if (existing == null) return;
            existing.setAttempts(existing.getAttempts() + 1);
            existing.setLastAttemptAt(now);
            repository.save(existing);
        });
        database.flushAsync();
    }

    private static NetworkEnvelope.Target parseTarget(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) return null;
        try {
            return NetworkEnvelope.Target.valueOf(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Set<String> normalizeAllowlist(Set<String> allowlist) {
        if (allowlist == null || allowlist.isEmpty()) return Set.of();
        HashSet<String> out = new HashSet<>();
        for (String ch : allowlist) {
            if (ch == null) continue;
            String trimmed = ch.trim();
            if (!trimmed.isBlank()) out.add(trimmed);
        }
        return Set.copyOf(out);
    }
}

