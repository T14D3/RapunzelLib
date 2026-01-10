package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryMessenger implements Messenger {
    private final String serverName;
    private final String proxyServerName;
    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

    public InMemoryMessenger(String serverName, String proxyServerName) {
        this.serverName = serverName;
        this.proxyServerName = proxyServerName;
    }

    @Override
    public void sendToAll(@NotNull String channel, @NotNull String data) {
        deliver(channel, data, serverName);
    }

    @Override
    public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
        if (serverName.isBlank()) return;
        if (!serverName.equalsIgnoreCase(this.serverName)) return;
        deliver(channel, data, this.serverName);
    }

    @Override
    public void sendToProxy(@NotNull String channel, @NotNull String data) {
        if (proxyServerName == null || proxyServerName.isBlank()) return;
        if (!proxyServerName.equalsIgnoreCase(this.serverName)) return;
        deliver(channel, data, serverName);
    }

    @Override
    public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
        listeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
        List<MessageListener> list = listeners.get(channel);
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

    private void deliver(String channel, String data, String sourceServer) {
        List<MessageListener> list = listeners.get(channel);
        if (list == null || list.isEmpty()) return;
        for (MessageListener listener : List.copyOf(list)) {
            listener.onMessage(channel, data, sourceServer);
        }
    }
}

