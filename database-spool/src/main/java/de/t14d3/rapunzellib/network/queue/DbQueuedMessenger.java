package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Simple DB-backed outbox wrapper for {@link Messenger}.
 *
 * <p>When immediate delivery isn't possible, selected channels are persisted to a shared DB and retried
 * periodically.</p>
 */
public final class DbQueuedMessenger implements Messenger, AutoCloseable {   
    public interface Listener {
        default void onEnqueued(long id, NetworkEnvelope.Target target, String targetServer, String channel) {
        }

        default void onDelivered(long id, NetworkEnvelope.Target target, String targetServer, String channel) {
        }

        default void onDropped(long id, DropReason reason, NetworkEnvelope.Target target, String targetServer, String channel) {
        }

        default void onExpired(long id, NetworkEnvelope.Target target, String targetServer, String channel, long ageMillis) {
        }

        default void onDeliveryFailed(long id, NetworkEnvelope.Target target, String targetServer, String channel, Exception error) {
        }
    }

    public enum DropReason {
        NOT_ALLOWLISTED,
        INVALID_TARGET,
        MISSING_TARGET_SERVER
    }

    interface OutboxStore {
        long enqueue(
            String ownerId,
            NetworkEnvelope.Target target,
            String targetServer,
            String channel,
            String data,
            long createdAt
        );

        List<StoredMessage> fetchBatch(String ownerId, int limit);

        void deleteByIds(List<Long> ids);

        void recordAttempt(long id, long now);
    }

    record StoredMessage(
        long id,
        String ownerId,
        String target,
        String targetServer,
        String channel,
        String data,
        long createdAt,
        int attempts,
        long lastAttemptAt
    ) {
    }

    static final class DatabaseOutboxStore implements OutboxStore, AutoCloseable {
        private final SpoolDatabase database;
        private final NetworkOutboxRepository repository;

        DatabaseOutboxStore(SpoolDatabase database) {
            this.database = Objects.requireNonNull(database, "database");
            this.repository = new NetworkOutboxRepository(database.entityManager());
        }

        @Override
        public long enqueue(
            String ownerId,
            NetworkEnvelope.Target target,
            String targetServer,
            String channel,
            String data,
            long createdAt
        ) {
            final long[] id = new long[1];
            database.runLocked(() -> {
                NetworkOutboxMessage msg = new NetworkOutboxMessage();
                msg.setOwnerId(ownerId);
                msg.setChannel(channel);
                msg.setData(data);
                msg.setTarget(target.name());
                msg.setTargetServer(targetServer);
                msg.setCreatedAt(createdAt);
                msg.setAttempts(0);
                msg.setLastAttemptAt(0L);

                repository.save(msg);
                database.entityManager().flush();
                id[0] = msg.getId();
            });
            return id[0];
        }

        @Override
        public List<StoredMessage> fetchBatch(String ownerId, int limit) {
            if (limit <= 0) return List.of();
            return database.locked(() -> repository
                .findBy("ownerId", ownerId)
                .stream()
                .sorted(Comparator.comparingLong(NetworkOutboxMessage::getId))
                .limit(limit)
                .map(msg -> new StoredMessage(
                    msg.getId(),
                    msg.getOwnerId(),
                    msg.getTarget(),
                    msg.getTargetServer(),
                    msg.getChannel(),
                    msg.getData(),
                    msg.getCreatedAt(),
                    msg.getAttempts(),
                    msg.getLastAttemptAt()
                ))
                .toList());
        }

        @Override
        public void deleteByIds(List<Long> ids) {
            if (ids == null || ids.isEmpty()) return;
            database.runLocked(() -> {
                for (Long id : ids) {
                    if (id == null) continue;
                    repository.deleteById(id);
                }
            });
            database.flushAsync();
        }

        @Override
        public void recordAttempt(long id, long now) {
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

        @Override
        public void close() throws Exception {
            database.close();
        }
    }

    static final class InMemoryOutboxStore implements OutboxStore {
        private final AtomicLong ids = new AtomicLong(0L);
        private final ConcurrentHashMap<Long, StoredMessage> messages = new ConcurrentHashMap<>();

        @Override
        public long enqueue(
            String ownerId,
            NetworkEnvelope.Target target,
            String targetServer,
            String channel,
            String data,
            long createdAt
        ) {
            long id = ids.incrementAndGet();
            messages.put(id, new StoredMessage(
                id,
                ownerId,
                target.name(),
                targetServer,
                channel,
                data,
                createdAt,
                0,
                0L
            ));
            return id;
        }

        @Override
        public List<StoredMessage> fetchBatch(String ownerId, int limit) {
            if (limit <= 0) return List.of();
            if (ownerId == null || ownerId.isBlank()) return List.of();
            return messages.values().stream()
                .filter(m -> ownerId.equals(m.ownerId()))
                .sorted(Comparator.comparingLong(StoredMessage::id))
                .limit(limit)
                .toList();
        }

        @Override
        public void deleteByIds(List<Long> ids) {
            if (ids == null || ids.isEmpty()) return;
            for (Long id : ids) {
                if (id == null) continue;
                messages.remove(id);
            }
        }

        @Override
        public void recordAttempt(long id, long now) {
            messages.computeIfPresent(id, (_id, existing) -> new StoredMessage(
                existing.id(),
                existing.ownerId(),
                existing.target(),
                existing.targetServer(),
                existing.channel(),
                existing.data(),
                existing.createdAt(),
                existing.attempts() + 1,
                now
            ));
        }

        int size() {
            return messages.size();
        }
    }

    private final OutboxStore store;
    private final Messenger delegate;
    private final Logger logger;
    private final Listener listener;
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
            allServersSupplier,
            canSendToServerOverride,
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
        Predicate<String> canSendToServerOverride,
        Listener listener
    ) {
        this(
            new DatabaseOutboxStore(Objects.requireNonNull(database, "database")),
            delegate,
            scheduler,
            logger,
            ownerId,
            channelAllowlist,
            flushPeriod,
            maxBatchSize,
            maxAge,
            allServersSupplier,
            canSendToServerOverride,
            listener
        );
    }

    DbQueuedMessenger(
        OutboxStore store,
        Messenger delegate,
        Scheduler scheduler,
        Logger logger,
        String ownerId,
        Set<String> channelAllowlist,
        Duration flushPeriod,
        int maxBatchSize,
        Duration maxAge,
        Supplier<List<String>> allServersSupplier,
        Predicate<String> canSendToServerOverride,
        Listener listener
    ) {
        this.store = Objects.requireNonNull(store, "store");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        Objects.requireNonNull(scheduler, "scheduler");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.listener = (listener != null) ? listener : new Listener() {
        };

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
    public void sendToAll(@NotNull String channel, @NotNull String data) {
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
    public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
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
    public void sendToProxy(@NotNull String channel, @NotNull String data) {
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
    public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
        delegate.registerListener(channel, listener);
    }

    @Override
    public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
        delegate.unregisterListener(channel, listener);
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public @NotNull String getServerName() {
        return delegate.getServerName();
    }

    @Override
    public @NotNull String getProxyServerName() {
        return delegate.getProxyServerName();
    }

    @Override
    public void close() {
        try {
            flushTask.cancel();
        } catch (Exception e) {
            logger.debug("Failed to cancel outbox flush task", e);
        }
        if (store instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.debug("Failed to close outbox store", e);
            }
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
            } catch (Exception e) {
                logger.debug("canSendToServer override failed for {}", normalized, e);
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
        } catch (Exception e) {
            logger.debug("allServersSupplier failed", e);
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
        final String targetServerValue =
            (normalizedTargetServer == null || normalizedTargetServer.isBlank()) ? null : normalizedTargetServer;

        long id = store.enqueue(ownerId, target, targetServerValue, normalizedChannel, payload, now);
        listener.onEnqueued(id, target, targetServerValue, normalizedChannel);
    }

    private void flush() {
        if (channelAllowlist.isEmpty()) return;
        if (maxBatchSize <= 0) return;
        if (!flushing.compareAndSet(false, true)) return;

        try {
            flushOnce();
        } catch (Exception e) {
            logger.debug("Outbox flush failed", e);
        } finally {
            flushing.set(false);
        }
    }

    private void flushOnce() {
        List<StoredMessage> batch = store.fetchBatch(ownerId, maxBatchSize);

        if (batch.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<Long> deleteIds = new ArrayList<>();

        for (StoredMessage msg : batch) {
            if (msg == null) continue;

            if (maxAgeMillis > 0L && msg.createdAt() > 0L && (now - msg.createdAt()) > maxAgeMillis) {
                deleteIds.add(msg.id());
                listener.onExpired(msg.id(), parseTargetOrNull(msg.target()), msg.targetServer(), msg.channel(), now - msg.createdAt());
                continue;
            }

            String channel = msg.channel();
            if (!shouldQueue(channel)) {
                deleteIds.add(msg.id());
                listener.onDropped(msg.id(), DropReason.NOT_ALLOWLISTED, parseTargetOrNull(msg.target()), msg.targetServer(), channel);
                continue;
            }

            NetworkEnvelope.Target target;
            try {
                target = parseTarget(msg.target());
            } catch (Exception e) {
                deleteIds.add(msg.id());
                listener.onDropped(msg.id(), DropReason.INVALID_TARGET, null, msg.targetServer(), channel);
                continue;
            }

            if (target == NetworkEnvelope.Target.SERVER
                && (msg.targetServer() == null || msg.targetServer().isBlank())) {
                deleteIds.add(msg.id());
                listener.onDropped(msg.id(), DropReason.MISSING_TARGET_SERVER, target, null, channel);
                continue;
            }

            if (!canSend(target, msg.targetServer())) {
                continue;
            }

            try {
                deliver(target, msg.targetServer(), channel, msg.data());
                deleteIds.add(msg.id());
                listener.onDelivered(msg.id(), target, msg.targetServer(), channel);
            } catch (Exception e) {
                listener.onDeliveryFailed(msg.id(), target, msg.targetServer(), channel, e);
                store.recordAttempt(msg.id(), now);
            }
        }

        if (!deleteIds.isEmpty()) {
            store.deleteByIds(deleteIds);
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
        store.recordAttempt(id, now);
    }

    private static NetworkEnvelope.Target parseTarget(String raw) {
        if (raw == null) throw new IllegalArgumentException("target is null");
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) throw new IllegalArgumentException("target is blank");
        return NetworkEnvelope.Target.valueOf(normalized);
    }

    private static NetworkEnvelope.Target parseTargetOrNull(String raw) {
        try {
            return parseTarget(raw);
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
