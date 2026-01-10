package de.t14d3.rapunzellib.network;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.json.GsonJsonCodec;
import de.t14d3.rapunzellib.network.json.JsonCodec;
import de.t14d3.rapunzellib.network.json.JsonCodecs;

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
    private final JsonCodec json;

    private final Map<String, CopyOnWriteArrayList<TypedRegistration<?>>> typedListeners = new ConcurrentHashMap<>();
    private final Map<String, MessageListener> rawBridgeListeners = new ConcurrentHashMap<>();

    public NetworkEventBus(Messenger messenger) {
        this(messenger, JsonCodecs.codec());
    }

    public NetworkEventBus(Messenger messenger, Gson gson) {
        this(messenger, new GsonJsonCodec(gson));
    }

    public NetworkEventBus(Messenger messenger, JsonCodec json) {
        this.messenger = messenger;
        this.json = json;
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

        for (TypedRegistration<?> reg : regs) {
            reg.dispatch(json, data, serverName);
        }
    }

    private record TypedRegistration<T>(Class<T> type, TypedListener<T> listener) {

        private void dispatch(JsonCodec json, String data, String serverName) {
                T payload = json.fromJson(data, type);
                listener.onEvent(payload, serverName);
            }
        }
}

