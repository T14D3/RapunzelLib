package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.Nullable;

public class NetworkEnvelope {
    public enum Target {
        PROXY,
        ALL,
        SERVER
    }

    private String channel;
    private String data;
    private Target target;
    private String targetServer;
    private String sourceServer;
    private long createdAt;

    public NetworkEnvelope() {
    }

    public NetworkEnvelope(String channel, String data, Target target, String targetServer, String sourceServer, long createdAt) {
        this.channel = channel;
        this.data = data;
        this.target = target;
        this.targetServer = targetServer;
        this.sourceServer = sourceServer;
        this.createdAt = createdAt;
    }

    public @Nullable String getChannel() {
        return channel;
    }

    public @Nullable String getData() {
        return data;
    }

    public @Nullable Target getTarget() {
        return target;
    }

    public @Nullable String getTargetServer() {
        return targetServer;
    }

    public @Nullable String getSourceServer() {
        return sourceServer;
    }

    public void setSourceServer(@Nullable String sourceServer) {
        this.sourceServer = sourceServer;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

