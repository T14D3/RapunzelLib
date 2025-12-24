package de.t14d3.rapunzellib.network.queue;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

@Entity
@Table(name = "network_outbox")
public class NetworkOutboxMessage {
    @Id(autoIncrement = true)
    @Column(name = "id")
    private long id;

    @Column(name = "owner_id", nullable = false, type = "VARCHAR(512)")
    private String ownerId;

    @Column(name = "channel", nullable = false, type = "VARCHAR(128)")
    private String channel;

    @Column(name = "data", nullable = false, type = "TEXT")
    private String data;

    @Column(name = "target", nullable = false, type = "VARCHAR(16)")
    private String target;

    @Column(name = "target_server", nullable = true, type = "VARCHAR(128)")
    private String targetServer;

    @Column(name = "created_at", nullable = false, type = "BIGINT")
    private long createdAt;

    @Column(name = "attempts", nullable = false, type = "INT")
    private int attempts;

    @Column(name = "last_attempt_at", nullable = false, type = "BIGINT")
    private long lastAttemptAt;

    public NetworkOutboxMessage() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public long getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(long lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
}

