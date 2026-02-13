package de.t14d3.rapunzellib.network;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.json.GsonJsonCodec;
import de.t14d3.rapunzellib.network.json.JsonCodec;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Bus for distributing network events across servers.
 * Supports both synchronous and asynchronous event dispatch for improved performance.
 */
public final class NetworkEventBus {
    private static final Logger logger = LoggerFactory.getLogger(NetworkEventBus.class);

    public interface TypedListener<T> {
        void onEvent(T payload, String sourceServer);
    }

    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    private final Messenger messenger;
    private final JsonCodec json;
    private final ExecutorService dispatcher;

    private final Map<String, CopyOnWriteArrayList<TypedRegistration<?>>> typedListeners = new ConcurrentHashMap<>();
    private final Map<String, MessageListener> rawBridgeListeners = new ConcurrentHashMap<>();
    private final Map<String, Boolean> asyncChannels = new ConcurrentHashMap<>();

    public NetworkEventBus(Messenger messenger) {
        this(messenger, JsonCodecs.codec(), ForkJoinPool.commonPool());
    }

    public NetworkEventBus(Messenger messenger, Gson gson) {
        this(messenger, new GsonJsonCodec(gson), ForkJoinPool.commonPool());
    }

    public NetworkEventBus(Messenger messenger, JsonCodec json) {
        this(messenger, json, ForkJoinPool.commonPool());
    }

    public NetworkEventBus(Messenger messenger, JsonCodec json, ExecutorService dispatcher) {
        this.messenger = messenger;
        this.json = json;
        this.dispatcher = dispatcher;
    }

    /**
     * Creates a new builder for configuring the NetworkEventBus.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Registers a typed listener with optional async dispatch.
     *
     * @param channel the channel to listen on
     * @param payloadType the type of payload
     * @param listener the listener to register
     * @param async whether to dispatch events asynchronously
     * @return a subscription handle
     */
    public <T> Subscription register(String channel, Class<T> payloadType, TypedListener<T> listener, boolean async) {
        CopyOnWriteArrayList<TypedRegistration<?>> list = typedListeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>());
        TypedRegistration<T> reg = new TypedRegistration<>(payloadType, listener);
        list.add(reg);

        asyncChannels.put(channel, async);

        rawBridgeListeners.computeIfAbsent(channel, ch -> {
            MessageListener raw = this::dispatchTyped;
            messenger.registerListener(ch, raw);
            return raw;
        });

        return () -> {
            CopyOnWriteArrayList<TypedRegistration<?>> current = typedListeners.get(channel);
            if (current != null) {
                current.remove(reg);
                if (current.isEmpty()) {
                    typedListeners.remove(channel);
                    asyncChannels.remove(channel);
                }
            }

            CopyOnWriteArrayList<TypedRegistration<?>> remaining = typedListeners.get(channel);
            if (remaining == null || remaining.isEmpty()) {
                MessageListener raw = rawBridgeListeners.remove(channel);
                if (raw != null) {
                    messenger.unregisterListener(channel, raw);
                }
            }
        };
    }

    /**
     * Registers a typed listener with synchronous dispatch (default behavior).
     */
    public <T> Subscription register(String channel, Class<T> payloadType, TypedListener<T> listener) {
        return register(channel, payloadType, listener, false);
    }

    public void sendToAll(String channel, Object payload) {
        messenger.sendToAll(channel, json.toJson(payload));
    }

    public void sendToServer(String channel, String serverName, Object payload) {
        messenger.sendToServer(channel, serverName, json.toJson(payload));
    }

    public void sendToProxy(String channel, Object payload) {
        messenger.sendToProxy(channel, json.toJson(payload));
    }

    private void dispatchTyped(String channel, String data, String serverName) {
        List<TypedRegistration<?>> regs = typedListeners.get(channel);
        if (regs == null || regs.isEmpty()) return;

        boolean asyncDispatch = asyncChannels.getOrDefault(channel, false);

        if (asyncDispatch) {
            // Dispatch in parallel using ExecutorService
            @SuppressWarnings("rawtypes")
            List<CompletableFuture> futures = regs.stream()
                .map(reg -> CompletableFuture.runAsync(() -> {
                    try {
                        reg.dispatch(json, data, serverName);
                    } catch (Exception e) {
                        logger.error("Async listener dispatch failed for channel {} from server {}",
                                     channel, serverName, e);
                    }
                }, dispatcher))
                .collect(Collectors.toList());
        } else {
            // Synchronous dispatch (existing behavior)
            for (TypedRegistration<?> reg : regs) {
                try {
                    reg.dispatch(json, data, serverName);
                } catch (Exception e) {
                    logger.error("Synchronous listener dispatch failed for channel {} from server {}",
                                 channel, serverName, e);
                }
            }
        }
    }

    private record TypedRegistration<T>(Class<T> type, TypedListener<T> listener) {

        private void dispatch(JsonCodec json, String data, String serverName) {
            T payload = json.fromJson(data, type);
            listener.onEvent(payload, serverName);
        }
    }

    /**
     * Builder for creating configured NetworkEventBus instances.
     */
    public static class Builder {
        private Messenger messenger;
        private JsonCodec json;
        private ExecutorService dispatcher;

        public Builder messenger(Messenger messenger) {
            this.messenger = messenger;
            return this;
        }

        public Builder json(JsonCodec json) {
            this.json = json;
            return this;
        }

        public Builder gson(Gson gson) {
            this.json = new GsonJsonCodec(gson);
            return this;
        }

        public Builder dispatcher(ExecutorService dispatcher) {
            this.dispatcher = dispatcher;
            return this;
        }

        public NetworkEventBus build() {
            if (messenger == null) {
                throw new IllegalStateException("messenger is required");
            }
            JsonCodec codec = json != null ? json : JsonCodecs.codec();
            ExecutorService executor = dispatcher != null ? dispatcher : ForkJoinPool.commonPool();
            return new NetworkEventBus(messenger, codec, executor);
        }
    }
}
