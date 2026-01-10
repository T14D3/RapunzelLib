package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DbQueuedMessengerTest {
    @Test
    void flushDeliversQueuedMessagesWhenDelegateBecomesConnected() {
        DbQueuedMessenger.InMemoryOutboxStore store = new DbQueuedMessenger.InMemoryOutboxStore();
        TestScheduler scheduler = new TestScheduler();
        CapturingMessenger delegate = new CapturingMessenger(false);
        RecordingListener listener = new RecordingListener();

        DbQueuedMessenger messenger = new DbQueuedMessenger(
            store,
            delegate,
            scheduler,
            LoggerFactory.getLogger("db-queue-test"),
            "owner",
            Set.of("ch"),
            Duration.ofSeconds(1),
            100,
            Duration.ofSeconds(10),
            null,
            null,
            listener
        );

        messenger.sendToServer("ch", "serverA", "hello");
        assertEquals(1, store.size());

        scheduler.flush.run();
        assertEquals(1, store.size());
        assertEquals(0, delegate.sent.size());

        delegate.connected = true;
        scheduler.flush.run();
        assertEquals(0, store.size());
        assertEquals(List.of("serverA|ch|hello"), delegate.sent);
        assertEquals(1, listener.delivered.size());
    }

    @Test
    void flushRespectsMaxBatchSize() {
        DbQueuedMessenger.InMemoryOutboxStore store = new DbQueuedMessenger.InMemoryOutboxStore();
        TestScheduler scheduler = new TestScheduler();
        CapturingMessenger delegate = new CapturingMessenger(false);

        DbQueuedMessenger messenger = new DbQueuedMessenger(
            store,
            delegate,
            scheduler,
            LoggerFactory.getLogger("db-queue-test"),
            "owner",
            Set.of("ch"),
            Duration.ofSeconds(1),
            1,
            Duration.ofSeconds(10),
            null,
            null,
            null
        );

        messenger.sendToServer("ch", "serverA", "a");
        messenger.sendToServer("ch", "serverA", "b");
        assertEquals(2, store.size());

        delegate.connected = true;
        scheduler.flush.run();
        assertEquals(1, store.size());
        assertEquals(1, delegate.sent.size());

        scheduler.flush.run();
        assertEquals(0, store.size());
        assertEquals(2, delegate.sent.size());
    }

    @Test
    void flushExpiresOldMessages() {
        DbQueuedMessenger.InMemoryOutboxStore store = new DbQueuedMessenger.InMemoryOutboxStore();
        TestScheduler scheduler = new TestScheduler();
        CapturingMessenger delegate = new CapturingMessenger(true);
        RecordingListener listener = new RecordingListener();

        long createdAt = System.currentTimeMillis() - 10_000L;
        store.enqueue("owner", NetworkEnvelope.Target.SERVER, "serverA", "ch", "hello", createdAt);
        assertEquals(1, store.size());

        DbQueuedMessenger messenger = new DbQueuedMessenger(
            store,
            delegate,
            scheduler,
            LoggerFactory.getLogger("db-queue-test"),
            "owner",
            Set.of("ch"),
            Duration.ofSeconds(1),
            100,
            Duration.ofMillis(1),
            null,
            null,
            listener
        );

        scheduler.flush.run();
        assertEquals(0, store.size());
        assertEquals(0, delegate.sent.size());
        assertEquals(1, listener.expired.size());
    }

    @Test
    void flushDropsMessagesWhenAllowlistChanges() {
        DbQueuedMessenger.InMemoryOutboxStore store = new DbQueuedMessenger.InMemoryOutboxStore();
        TestScheduler scheduler = new TestScheduler();
        CapturingMessenger delegate = new CapturingMessenger(true);
        RecordingListener listener = new RecordingListener();

        store.enqueue("owner", NetworkEnvelope.Target.SERVER, "serverA", "other", "hello", System.currentTimeMillis());
        assertEquals(1, store.size());

        DbQueuedMessenger messenger = new DbQueuedMessenger(
            store,
            delegate,
            scheduler,
            LoggerFactory.getLogger("db-queue-test"),
            "owner",
            Set.of("ch"),
            Duration.ofSeconds(1),
            100,
            Duration.ofSeconds(10),
            null,
            null,
            listener
        );

        scheduler.flush.run();
        assertEquals(0, store.size());
        assertEquals(1, listener.dropped.size());
        assertEquals(DbQueuedMessenger.DropReason.NOT_ALLOWLISTED, listener.dropped.getFirst().reason);
    }

    private static final class TestScheduler implements Scheduler {
        private final ScheduledTask task = new ScheduledTask() {
            @Override
            public void cancel() {
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

        private Runnable flush;

        @Override
        public @NotNull ScheduledTask run(@NotNull Runnable task) {
            task.run();
            return this.task;
        }

        @Override
        public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
            task.run();
            return this.task;
        }

        @Override
        public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
            return this.task;
        }

        @Override
        public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            flush = task;
            return this.task;
        }

        @Override
        public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            flush = task;
            return this.task;
        }
    }

    private static final class CapturingMessenger implements Messenger {
        private volatile boolean connected;
        private final List<String> sent = new ArrayList<>();

        private CapturingMessenger(boolean connected) {
            this.connected = connected;
        }

        @Override
        public void sendToAll(@NotNull String channel, @NotNull String data) {
            sent.add("*|" + channel + "|" + data);
        }

        @Override
        public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
            sent.add(serverName + "|" + channel + "|" + data);
        }

        @Override
        public void sendToProxy(@NotNull String channel, @NotNull String data) {
            sent.add("proxy|" + channel + "|" + data);
        }

        @Override
        public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
        }

        @Override
        public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public @NotNull String getServerName() {
            return "self";
        }

        @Override
        public @NotNull String getProxyServerName() {
            return "proxy";
        }
    }

    private static final class RecordingListener implements DbQueuedMessenger.Listener {
        private final List<Long> delivered = new CopyOnWriteArrayList<>();
        private final List<Long> expired = new CopyOnWriteArrayList<>();
        private final List<Dropped> dropped = new CopyOnWriteArrayList<>();

        @Override
        public void onDelivered(long id, NetworkEnvelope.Target target, String targetServer, String channel) {
            delivered.add(id);
        }

        @Override
        public void onExpired(long id, NetworkEnvelope.Target target, String targetServer, String channel, long ageMillis) {
            expired.add(id);
        }

        @Override
        public void onDropped(long id, DbQueuedMessenger.DropReason reason, NetworkEnvelope.Target target, String targetServer, String channel) {
            dropped.add(new Dropped(id, reason));
        }

        private record Dropped(long id, DbQueuedMessenger.DropReason reason) {
        }
    }
}
