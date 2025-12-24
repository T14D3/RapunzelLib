package de.t14d3.rapunzellib.network;

public interface Messenger {
    void sendToAll(String channel, String data);

    void sendToServer(String channel, String serverName, String data);

    void sendToProxy(String channel, String data);

    void registerListener(String channel, MessageListener listener);

    void unregisterListener(String channel, MessageListener listener);

    boolean isConnected();

    String getServerName();

    String getProxyServerName();
}

