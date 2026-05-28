package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class MessageBuffer {
    private final Deque<BufferedMessage> queue = new ArrayDeque<>();
    private final int maxSize;

    public MessageBuffer() {
        this(1024);
    }

    public MessageBuffer(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be positive");
        this.maxSize = maxSize;
    }

    public synchronized boolean enqueue(@NotNull String channel, @NotNull String data,
                                         @NotNull String targetServer, @NotNull Target target) {
        if (queue.size() >= maxSize) {
            queue.pollFirst();
        }
        return queue.offerLast(new BufferedMessage(channel, data, targetServer, target));
    }

    public synchronized int drainTo(@NotNull Messenger messenger) {
        if (queue.isEmpty()) return 0;
        List<BufferedMessage> batch = new ArrayList<>(queue);
        queue.clear();

        int delivered = 0;
        for (BufferedMessage msg : batch) {
            try {
                switch (msg.target) {
                    case ALL -> messenger.sendToAll(msg.channel, msg.data);
                    case PROXY -> messenger.sendToProxy(msg.channel, msg.data);
                    case SERVER -> messenger.sendToServer(msg.channel, msg.targetServer, msg.data);
                }
                delivered++;
            } catch (Exception e) {
                enqueue(msg.channel, msg.data, msg.targetServer, msg.target);
            }
        }
        return delivered;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public enum Target { ALL, PROXY, SERVER }

    private record BufferedMessage(
        @NotNull String channel,
        @NotNull String data,
        @NotNull String targetServer,
        @NotNull Target target
    ) {
        BufferedMessage {
            Objects.requireNonNull(channel, "channel");
            Objects.requireNonNull(data, "data");
            Objects.requireNonNull(targetServer, "targetServer");
            Objects.requireNonNull(target, "target");
        }
    }
}
