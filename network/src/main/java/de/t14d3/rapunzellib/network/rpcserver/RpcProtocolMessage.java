package de.t14d3.rapunzellib.network.rpcserver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Protocol message types for RPC server communication.
 *
 * <p>Defines the message envelope structure used for communication between
 * the proxy (RPC server) and backend Minecraft servers (RPC clients).
 *
 * <p><strong>Message Types:</strong>
 * <ul>
 * <li><strong>HELLO:</strong> Initial handshake message where backend identifies itself</li>
 * <li><strong>MESSAGE:</strong> Actual data payload envelope for application messages</li>
 * <li><strong>HEARTBEAT:</strong> Keepalive message to maintain connection health</li>
 * <li><strong>DISCONNECT:</strong> Graceful disconnect notification</li>
 * </ul>
 *
 * <p><strong>Protocol Format:</strong>
 * <pre>
 * [4 bytes: length][JSON payload]
 * </pre>
 *
 * @since 1.0
 * @see RpcServerMessenger
 * @see BackendClientHandler
 */
public class RpcProtocolMessage {

    /**
     * Message type enumeration for protocol discrimination.
     */
    public enum Type {
        /**
         * Initial handshake message sent by backend to identify itself.
         * Contains serverName and version information.
         */
        HELLO,

        /**
         * Application message envelope containing actual payload data.
         * Used for routing messages between servers and proxy.
         */
        MESSAGE,

        /**
         * Keepalive message for connection health monitoring.
         * Sent periodically to detect connection failures.
         */
        HEARTBEAT,

        /**
         * Graceful disconnect notification.
         * Indicates intentional connection termination.
         */
        DISCONNECT
    }

    private Type type;
    private String serverName;
    private String version;
    private String channel;
    private String data;
    private String targetServer;
    private String sourceServer;
    private Long timestamp;

    /**
     * Default constructor for JSON deserialization.
     */
    public RpcProtocolMessage() {
    }

    /**
     * Creates a HELLO message for initial handshake.
     *
     * @param serverName the identifying name of the backend server
     * @param version    the protocol version
     * @return a new HELLO protocol message
     */
    public static RpcProtocolMessage hello(@NotNull String serverName, @NotNull String version) {
        RpcProtocolMessage msg = new RpcProtocolMessage();
        msg.type = Type.HELLO;
        msg.serverName = serverName;
        msg.version = version;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    /**
     * Creates a MESSAGE envelope for application data.
     *
     * @param channel      the message channel for routing
     * @param data         the serialized message payload
     * @param targetServer the target server name (null for broadcast/proxy)
     * @param sourceServer the originating server name
     * @return a new MESSAGE protocol message
     */
    public static RpcProtocolMessage message(@NotNull String channel, @NotNull String data,
                                              @Nullable String targetServer, @NotNull String sourceServer) {
        RpcProtocolMessage msg = new RpcProtocolMessage();
        msg.type = Type.MESSAGE;
        msg.channel = channel;
        msg.data = data;
        msg.targetServer = targetServer;
        msg.sourceServer = sourceServer;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    /**
     * Creates a HEARTBEAT message for connection health.
     *
     * @param serverName the server sending the heartbeat
     * @return a new HEARTBEAT protocol message
     */
    public static RpcProtocolMessage heartbeat(@NotNull String serverName) {
        RpcProtocolMessage msg = new RpcProtocolMessage();
        msg.type = Type.HEARTBEAT;
        msg.serverName = serverName;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    /**
     * Creates a DISCONNECT message for graceful termination.
     *
     * @param serverName the server disconnecting
     * @return a new DISCONNECT protocol message
     */
    public static RpcProtocolMessage disconnect(@NotNull String serverName) {
        RpcProtocolMessage msg = new RpcProtocolMessage();
        msg.type = Type.DISCONNECT;
        msg.serverName = serverName;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    public @Nullable Type getType() {
        return type;
    }

    public void setType(@Nullable Type type) {
        this.type = type;
    }

    public @Nullable String getServerName() {
        return serverName;
    }

    public void setServerName(@Nullable String serverName) {
        this.serverName = serverName;
    }

    public @Nullable String getVersion() {
        return version;
    }

    public void setVersion(@Nullable String version) {
        this.version = version;
    }

    public @Nullable String getChannel() {
        return channel;
    }

    public void setChannel(@Nullable String channel) {
        this.channel = channel;
    }

    public @Nullable String getData() {
        return data;
    }

    public void setData(@Nullable String data) {
        this.data = data;
    }

    public @Nullable String getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(@Nullable String targetServer) {
        this.targetServer = targetServer;
    }

    public @Nullable String getSourceServer() {
        return sourceServer;
    }

    public void setSourceServer(@Nullable String sourceServer) {
        this.sourceServer = sourceServer;
    }

    public @Nullable Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@Nullable Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns true if this message is a HELLO type.
     */
    public boolean isHello() {
        return type == Type.HELLO;
    }

    /**
     * Returns true if this message is a MESSAGE type.
     */
    public boolean isMessage() {
        return type == Type.MESSAGE;
    }

    /**
     * Returns true if this message is a HEARTBEAT type.
     */
    public boolean isHeartbeat() {
        return type == Type.HEARTBEAT;
    }

    /**
     * Returns true if this message is a DISCONNECT type.
     */
    public boolean isDisconnect() {
        return type == Type.DISCONNECT;
    }
}
