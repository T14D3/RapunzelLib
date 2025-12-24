package de.t14d3.rapunzellib.network;

public interface MessageListener {
    void onMessage(String channel, String data, String sourceServer);
}

