package de.t14d3.rapunzellib.network.rpc;

import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

final class RpcClientTest {
    @Test
    void callServerReceivesResponseAndCancelsTimeoutTask() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();

        Messenger clientMessenger = network.create("client", "proxy");
        Messenger serverMessenger = network.create("server", "proxy");

        NetworkEventBus serverBus = new NetworkEventBus(serverMessenger, JsonCodecs.gson());
        serverBus.register(RpcChannels.REQUEST, RpcRequest.class, (req, sourceServer) -> {
            RpcResponse response = new RpcResponse(
                req.requestId(),
                true,
                JsonCodecs.gson().toJsonTree("ok"),
                null,
                System.currentTimeMillis()
            );
            serverBus.sendToServer(RpcChannels.RESPONSE, sourceServer, response);
        });

        RpcClient client = new RpcClient(clientMessenger, scheduler, LoggerFactory.getLogger("rpc-client"), Duration.ofSeconds(1));
        CompletableFuture<String> future = client.callServer("server", "svc", "m", Map.of("x", 1), String.class);
        assertEquals("ok", future.join());

        assertEquals(1, scheduler.scheduled.size());
        assertTrue(scheduler.scheduled.getFirst().isCancelled());
    }

    @Test
    void callServerTimesOutAndCompletesExceptionally() {
        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();

        Messenger clientMessenger = network.create("client", "proxy");

        RpcClient client = new RpcClient(clientMessenger, scheduler, LoggerFactory.getLogger("rpc-client"), Duration.ofSeconds(1));
        CompletableFuture<String> future = client.callServer("server", "svc", "m", Map.of("x", 1), String.class);

        scheduler.triggerAll();
        CompletionException ex = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(TimeoutException.class, ex.getCause());
        assertNotNull(ex.getCause().getMessage());
    }

    private static final class TestScheduler implements Scheduler {
        private final List<TestTask> scheduled = new CopyOnWriteArrayList<>();

        @Override
        public @NotNull ScheduledTask run(@NotNull Runnable task) {
            task.run();
            return new TestTask(null);
        }

        @Override
        public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
            task.run();
            return new TestTask(null);
        }

        @Override
        public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
            TestTask t = new TestTask(task);
            scheduled.add(t);
            return t;
        }

        @Override
        public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            return new TestTask(null);
        }

        @Override
        public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            return new TestTask(null);
        }

        void triggerAll() {
            for (TestTask task : List.copyOf(scheduled)) {
                if (task.isCancelled()) continue;
                if (task.runnable != null) task.runnable.run();
            }
        }

        private static final class TestTask implements ScheduledTask {
            private final Runnable runnable;
            private volatile boolean cancelled;

            private TestTask(Runnable runnable) {
                this.runnable = runnable;
            }

            @Override
            public void cancel() {
                cancelled = true;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        }
    }

    private static final class TestNetwork {
        private final Map<String, Node> nodes = new ConcurrentHashMap<>();

        Messenger create(String serverName, String proxyServerName) {
            Node node = nodes.computeIfAbsent(serverName, _n -> new Node());
            return new Messenger() {
                @Override
                public void sendToAll(@NotNull String channel, @NotNull String data) {
                    for (var entry : nodes.entrySet()) {
                        entry.getValue().deliver(channel, data, serverName);
                    }
                }

                @Override
                public void sendToServer(@NotNull String channel, @NotNull String targetServerName, @NotNull String data) {
                    if (targetServerName == null || targetServerName.isBlank()) return;
                    Node target = nodes.get(targetServerName);
                    if (target == null) return;
                    target.deliver(channel, data, serverName);
                }

                @Override
                public void sendToProxy(@NotNull String channel, @NotNull String data) {
                    sendToServer(channel, proxyServerName, data);
                }

                @Override
                public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
                    node.listeners.computeIfAbsent(channel, _c -> new CopyOnWriteArrayList<>()).add(listener);
                }

                @Override
                public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
                    List<MessageListener> list = node.listeners.get(channel);
                    if (list == null) return;
                    list.remove(listener);
                }

                @Override
                public boolean isConnected() {
                    return true;
                }

                @Override
                public @NotNull String getServerName() {
                    return serverName;
                }

                @Override
                public @NotNull String getProxyServerName() {
                    return proxyServerName;
                }
            };
        }

        private static final class Node {
            private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

            void deliver(String channel, String data, String sourceServer) {
                List<MessageListener> list = listeners.get(channel);
                if (list == null || list.isEmpty()) return;
                for (MessageListener listener : List.copyOf(list)) {
                    listener.onMessage(channel, data, sourceServer);
                }
            }
        }
    }
}
