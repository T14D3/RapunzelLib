package de.t14d3.rapunzellib.network.queue;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

/**
 * Database entity representing a single outbox message in the {@code network_outbox} table.
 * <p>
 * Each message records the target, channel, payload, creation time, and delivery
 * attempt tracking for the store-and-forward network queue.
 * </p>
 */
@Entity
@Table(name = "network_outbox")
public class NetworkOutboxMessage {
    /** Auto-increment primary key. */
    @Id(autoIncrement = true)
    @Column(name = "id")
    private long id;

    /** Identifier of the owning plugin/server instance. */
    @Column(name = "owner_id", nullable = false, type = "VARCHAR(512)")
    private String ownerId;

    /** The message channel name. */
    @Column(name = "channel", nullable = false, type = "VARCHAR(128)")
    private String channel;

    /** The message payload data. */
    @Column(name = "data", nullable = false, type = "TEXT")
    private String data;

    /** The delivery target type (ALL, PROXY, SERVER). */
    @Column(name = "target", nullable = false, type = "VARCHAR(16)")
    private String target;

    /** The target server name (for SERVER-targeted messages). */
    @Column(name = "target_server", nullable = true, type = "VARCHAR(128)")
    private String targetServer;

    /** Timestamp (epoch millis) when the message was created. */
    @Column(name = "created_at", nullable = false, type = "BIGINT")
    private long createdAt;

    /** Number of delivery attempts made so far. */
    @Column(name = "attempts", nullable = false, type = "INT")
    private int attempts;

    /** Timestamp (epoch millis) of the last delivery attempt. */
    @Column(name = "last_attempt_at", nullable = false, type = "BIGINT")
    private long lastAttemptAt;

    /** Default constructor required by the ORM. */
    public NetworkOutboxMessage() {
    }

    /**
     * Gets the message ID.
     *
     * @return the primary key
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the message ID.
     *
     * @param id the primary key
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Gets the owner identifier.
     *
     * @return the owner ID
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Sets the owner identifier.
     *
     * @param ownerId the owner ID
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Gets the message channel.
     *
     * @return the channel name
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the message channel.
     *
     * @param channel the channel name
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Gets the message payload data.
     *
     * @return the payload string
     */
    public String getData() {
        return data;
    }

    /**
     * Sets the message payload data.
     *
     * @param data the payload string
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Gets the delivery target type string.
     *
     * @return the target (ALL, PROXY, or SERVER)
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the delivery target type.
     *
     * @param target the target string
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Gets the target server name.
     *
     * @return the server name, or null
     */
    public String getTargetServer() {
        return targetServer;
    }

    /**
     * Sets the target server name.
     *
     * @param targetServer the server name, or null
     */
    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return the epoch millis timestamp
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt the epoch millis timestamp
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the number of delivery attempts.
     *
     * @return the attempt count
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Sets the number of delivery attempts.
     *
     * @param attempts the attempt count
     */
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    /**
     * Gets the timestamp of the last delivery attempt.
     *
     * @return the epoch millis timestamp
     */
    public long getLastAttemptAt() {
        return lastAttemptAt;
    }

    /**
     * Sets the timestamp of the last delivery attempt.
     *
     * @param lastAttemptAt the epoch millis timestamp
     */
    public void setLastAttemptAt(long lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
}
