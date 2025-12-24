package de.t14d3.rapunzellib.network;

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
    public void sendToAll(String channel, String data) {
        deliver(channel, data, serverName);
    }

    @Override
    public void sendToServer(String channel, String serverName, String data) {
        deliver(channel, data, this.serverName);
    }

    @Override
    public void sendToProxy(String channel, String data) {
        deliver(channel, data, serverName);
    }

    @Override
    public void registerListener(String channel, MessageListener listener) {
        listeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterListener(String channel, MessageListener listener) {
        List<MessageListener> list = listeners.get(channel);
        if (list == null) return;
        list.remove(listener);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public String getProxyServerName() {
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

