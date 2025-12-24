package de.t14d3.rapunzellib.network;

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


    public String getChannel() {
        return channel;
    }

    public String getData() {
        return data;
    }

    public Target getTarget() {
        return target;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public String getSourceServer() {
        return sourceServer;
    }

    public void setSourceServer(String sourceServer) {
        this.sourceServer = sourceServer;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

