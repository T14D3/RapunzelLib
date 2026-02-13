package de.t14d3.rapunzellib.network.testutil;

import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntime;
import de.t14d3.rapunzellib.network.runtime.DefaultNetworkRuntimeGateway;
import de.t14d3.rapunzellib.network.runtime.NetworkLink;
import de.t14d3.rapunzellib.network.runtime.NetworkLinkKind;
import de.t14d3.rapunzellib.network.runtime.NetworkNodeRole;
import de.t14d3.rapunzellib.network.runtime.NetworkRuntime;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TestNetworkSupport {
    private TestNetworkSupport() {
    }

    public static final class TestScheduler implements Scheduler {
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
            TestTask testTask = new TestTask(task);
            scheduled.add(testTask);
            return testTask;
        }

        @Override
        public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            return new TestTask(null);
        }

        @Override
        public @NotNull ScheduledTask runRepeatingAsync(
            @NotNull Duration initialDelay,
            @NotNull Duration period,
            @NotNull Runnable task
        ) {
            return new TestTask(null);
        }

        public void triggerAll() {
            for (TestTask task : List.copyOf(scheduled)) {
                if (!task.isCancelled() && task.runnable != null) {
                    task.runnable.run();
                }
            }
        }

        public List<TestTask> scheduledTasks() {
            return scheduled;
        }

        public static final class TestTask implements ScheduledTask {
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

    public static final class TestNetwork {
        private final Map<String, Node> nodes = new ConcurrentHashMap<>();

        public Messenger createMessenger(String serverName, String proxyServerName) {
            Node node = nodes.computeIfAbsent(serverName, _ignored -> new Node());
            return new Messenger() {
                @Override
                public void sendToAll(@NotNull String channel, @NotNull String data) {
                    for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                        entry.getValue().deliver(channel, data, serverName);
                    }
                }

                @Override
                public void sendToServer(@NotNull String channel, @NotNull String targetServerName, @NotNull String data) {
                    Node target = nodes.get(targetServerName);
                    if (target != null) {
                        target.deliver(channel, data, serverName);
                    }
                }

                @Override
                public void sendToProxy(@NotNull String channel, @NotNull String data) {
                    sendToServer(channel, proxyServerName, data);
                }

                @Override
                public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
                    node.listeners.computeIfAbsent(channel, _ignored -> new CopyOnWriteArrayList<>()).add(listener);
                }

                @Override
                public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
                    List<MessageListener> listeners = node.listeners.get(channel);
                    if (listeners != null) {
                        listeners.remove(listener);
                    }
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

        public DefaultNetworkRuntimeGateway createGateway(String serverName, String proxyServerName, Scheduler scheduler) {
            Messenger messenger = createMessenger(serverName, proxyServerName);
            NetworkRuntime runtime = new DefaultNetworkRuntime(
                serverName.equalsIgnoreCase(proxyServerName) ? NetworkNodeRole.PROXY : NetworkNodeRole.BACKEND,
                serverName,
                proxyServerName,
                new NetworkLink(NetworkLinkKind.IN_MEMORY, messenger),
                Optional.empty(),
                messenger
            );
            return new DefaultNetworkRuntimeGateway(runtime, scheduler, LoggerFactory.getLogger(serverName + "-gateway"));
        }

        private static final class Node {
            private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

            private void deliver(String channel, String data, String sourceServer) {
                List<MessageListener> channelListeners = listeners.get(channel);
                if (channelListeners == null) {
                    return;
                }
                for (MessageListener listener : List.copyOf(channelListeners)) {
                    listener.onMessage(channel, data, sourceServer);
                }
            }
        }
    }
}
