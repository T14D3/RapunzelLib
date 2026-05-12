package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.Nullable;

/**
 * Envelope wrapper for network messages providing routing metadata and channel-based delivery.
 *
 * <p>The NetworkEnvelope implements the <strong>Envelope Pattern</strong> for network messaging,
 * wrapping raw message data with addressing information that enables flexible routing across
 * proxy-server boundaries in a multi-server Minecraft network architecture.
 *
 * <p><strong>Envelope Pattern for Network Messages:</strong>
 * <p>Each envelope encapsulates:
 * <ul>
 *   <li><strong>Channel:</strong> Logical destination channel for message routing</li>
 *   <li><strong>Data:</strong> Serialized message payload (typically JSON)</li>
 *   <li><strong>Target:</strong> Routing directive (PROXY, ALL servers, or specific SERVER)</li>
 *   <li><strong>TargetServer:</strong> Specific server identifier when Target is SERVER</li>
 *   <li><strong>SourceServer:</strong> Originating server identifier for response routing</li>
 *   <li><strong>CreatedAt:</strong> Timestamp for message ordering and timeout handling</li>
 * </ul>
 *
 * <p><strong>Serialization/Deserialization:</strong>
 * <p>Envelopes are designed for JSON serialization via Gson or similar libraries.
 * All fields have nullable getters and appropriate setters for flexible deserialization.
 * The default constructor enables reflection-based instantiation by serialization frameworks.
 *
 * <p><strong>Channel-Based Routing:</strong>
 * <p>Messages are routed based on their channel identifier, allowing multiple independent
 * communication streams over a shared transport. Channels enable:
 * <ul>
 *   <li>Separate namespaces for different subsystems (RPC, file sync, custom plugins)</li>
 *   <li>Selective subscription by interested components</li>
 *   <li>Message filtering at the transport layer</li>
 * </ul>
 *
 * <p><strong>Usage in Messenger System:</strong>
 * <pre>{@code
 * // Creating an envelope for broadcast to all servers
 * NetworkEnvelope envelope = new NetworkEnvelope(
 *     "myplugin:events",
 *     "{\"type\":\"player_join\",\"player\":\"Steve\"}",
 *     NetworkEnvelope.Target.ALL,
 *     null,
 *     "server1",
 *     System.currentTimeMillis()
 * );
 *
 * // Creating a targeted envelope for specific server
 * NetworkEnvelope directMessage = new NetworkEnvelope(
 *     "myplugin:commands",
 *     commandData,
 *     NetworkEnvelope.Target.SERVER,
 *     "lobby",
 *     "server1",
 *     System.currentTimeMillis()
 * );
 * }</pre>
 *
 * <p><strong>Implementation note:</strong> This class is intentionally mutable to support
 * deserialization frameworks. Thread safety is the responsibility of the caller; immutable copies
 * should be made if concurrent access is required.
 * @since 1.0
 * @see Messenger
 * @see NetworkEventBus
 * @see de.t14d3.rapunzellib.network.redis.RedisPubSubMessenger
 */
public class NetworkEnvelope {
    /**
     * Routing target for network message delivery.
     */
    public enum Target {
        /** Deliver to the proxy server only. */
        PROXY,
        /** Deliver to all servers (except the sender). */
        ALL,
        /** Deliver to a specific named server. */
        SERVER
    }

    private String channel;
    private String data;
    private Target target;
    private String targetServer;
    private String sourceServer;
    private long createdAt;

    /**
     * Default constructor for JSON deserialization frameworks.
     */
    public NetworkEnvelope() {
    }

    /**
     * Creates a fully specified network envelope.
     *
     * @param channel      the logical destination channel
     * @param data         the serialized message payload
     * @param target       the routing directive
     * @param targetServer the specific target server (may be null for PROXY/ALL targets)
     * @param sourceServer the originating server name
     * @param createdAt    the creation timestamp
     */
    public NetworkEnvelope(String channel, String data, Target target, String targetServer, String sourceServer, long createdAt) {
        this.channel = channel;
        this.data = data;
        this.target = target;
        this.targetServer = targetServer;
        this.sourceServer = sourceServer;
        this.createdAt = createdAt;
    }

    /**
     * Returns the logical destination channel.
     *
     * @return the channel, or null if not set
     */
    public @Nullable String getChannel() {
        return channel;
    }

    /**
     * Returns the serialized message payload.
     *
     * @return the data, or null if not set
     */
    public @Nullable String getData() {
        return data;
    }

    /**
     * Returns the routing directive.
     *
     * @return the target, or null if not set
     */
    public @Nullable Target getTarget() {
        return target;
    }

    /**
     * Returns the specific target server for SERVER-targeted messages.
     *
     * @return the target server name, or null if not applicable
     */
    public @Nullable String getTargetServer() {
        return targetServer;
    }

    /**
     * Returns the originating server name.
     *
     * @return the source server name, or null if not set
     */
    public @Nullable String getSourceServer() {
        return sourceServer;
    }

    /**
     * Sets the originating server name.
     *
     * @param sourceServer the source server name
     */
    public void setSourceServer(@Nullable String sourceServer) {
        this.sourceServer = sourceServer;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return the timestamp in milliseconds
     */
    public long getCreatedAt() {
        return createdAt;
    }
}
