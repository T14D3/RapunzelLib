package de.t14d3.rapunzellib.network;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NetworkEventBus {
    public interface TypedListener<T> {
        void onEvent(T payload, String sourceServer);
    }

    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    private final Messenger messenger;
    private final Gson gson;

    private final Map<String, CopyOnWriteArrayList<TypedRegistration<?>>> typedListeners = new ConcurrentHashMap<>();
    private final Map<String, MessageListener> rawBridgeListeners = new ConcurrentHashMap<>();

    public NetworkEventBus(Messenger messenger) {
        this(messenger, new Gson());
    }

    public NetworkEventBus(Messenger messenger, Gson gson) {
        this.messenger = messenger;
        this.gson = gson;
    }

    public <T> Subscription register(String channel, Class<T> payloadType, TypedListener<T> listener) {
        CopyOnWriteArrayList<TypedRegistration<?>> list = typedListeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>());
        TypedRegistration<T> reg = new TypedRegistration<>(payloadType, listener);
        list.add(reg);

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

    public void sendToAll(String channel, Object payload) {
        messenger.sendToAll(channel, gson.toJson(payload));
    }

    public void sendToServer(String channel, String serverName, Object payload) {
        messenger.sendToServer(channel, serverName, gson.toJson(payload));
    }

    public void sendToProxy(String channel, Object payload) {
        messenger.sendToProxy(channel, gson.toJson(payload));
    }

    private void dispatchTyped(String channel, String data, String serverName) {
        List<TypedRegistration<?>> regs = typedListeners.get(channel);
        if (regs == null || regs.isEmpty()) return;

        for (TypedRegistration<?> reg : regs) {
            reg.dispatch(gson, data, serverName);
        }
    }

    private static final class TypedRegistration<T> {
        private final Class<T> type;
        private final TypedListener<T> listener;

        private TypedRegistration(Class<T> type, TypedListener<T> listener) {
            this.type = type;
            this.listener = listener;
        }

        private void dispatch(Gson gson, String data, String serverName) {
            T payload = gson.fromJson(data, type);
            listener.onEvent(payload, serverName);
        }
    }
}

