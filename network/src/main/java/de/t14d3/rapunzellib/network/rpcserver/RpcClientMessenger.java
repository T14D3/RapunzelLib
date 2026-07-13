package de.t14d3.rapunzellib.network.rpcserver;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkDefaults;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC Client Messenger implementation for RapunzelLib.
 *
 * <p>Connects backend Minecraft servers (Paper/Sponge/etc) to the RpcServerMessenger
 * running on the proxy (Velocity). Uses TCP sockets with a custom protocol for
 * message exchange.
 *
 * <p><strong>Architecture:</strong>
 * <ul>
 * <li>Backend server creates a TCP socket connection to the proxy's RPC port</li>
 * <li>Sends HELLO message to identify itself to the proxy</li>
 * <li>Protocol: JSON-based messages with 4-byte length prefix for framing</li>
 * <li>Background thread reads messages continuously</li>
 * <li>Automatic reconnection with exponential backoff on connection failure</li>
 * </ul>
 *
 * <p><strong>Protocol Message Types:</strong>
 * <ul>
 * <li><strong>HELLO:</strong> Initial handshake message with server identification</li>
 * <li><strong>MESSAGE:</strong> Actual message envelope for application data</li>
 * <li><strong>HEARTBEAT:</strong> Keepalive for connection health</li>
 * <li><strong>DISCONNECT:</strong> Graceful disconnect notification</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * RpcClientConfig config = RpcClientConfig.builder("lobby-server")
 *     .proxyHost("proxy.example.com")
 *     .proxyPort(25566)
 *     .build();
 *
 * try (RpcClientMessenger messenger = new RpcClientMessenger(config)) {
 *     messenger.registerListener("mychannel", (channel, data, source) -> {
 *         System.out.println("Received from " + source + ": " + data);
 *     });
 *
 *     messenger.sendToAll("mychannel", "{\"message\":\"hello all\"}");
 *     messenger.sendToServer("mychannel", "minigames", "{\"message\":\"direct\"}");
 *     messenger.sendToProxy("mychannel", "{\"message\":\"to proxy\"}");
 * }
 * }</pre>
 *
 * @since 1.0
 * @see Messenger
 * @see RpcClientConfig
 * @see RpcProtocolMessage
 */
public class RpcClientMessenger implements Messenger, AutoCloseable {

    private final RpcClientConfig config;
    private final Logger logger;
    private final Gson gson = JsonCodecs.gson();

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

    private volatile Socket socket;
    private volatile DataInputStream input;
    private volatile DataOutputStream output;
    private volatile Thread readerThread;
    private volatile ExecutorService heartbeatExecutor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean identified = new AtomicBoolean(false);
    private final AtomicLong lastHeartbeat = new AtomicLong(0);
    private final AtomicLong lastActivity = new AtomicLong(0);

    private volatile String proxyServerName;
    private volatile long currentReconnectDelay;

    /**
     * Creates a new RPC client messenger with the specified configuration.
     *
     * @param config the RPC client configuration
     * @throws IllegalArgumentException if config is null
     */
    public RpcClientMessenger(@NotNull RpcClientConfig config) {
        this(config, LoggerFactory.getLogger(RpcClientMessenger.class));
    }

    /**
     * Creates a new RPC client messenger with custom logger.
     *
     * @param config the RPC client configuration
     * @param logger the logger instance
     * @throws IllegalArgumentException if config or logger is null
     */
    public RpcClientMessenger(@NotNull RpcClientConfig config, @NotNull Logger logger) {
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.currentReconnectDelay = config.reconnectDelayMillis();

        startClient();
    }

    private void startClient() {
        if (running.compareAndSet(false, true)) {
            connect();

            // Start heartbeat executor
            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "RapunzelLib-RPC-Heartbeat");
                t.setDaemon(true);
                return t;
            });

            ((java.util.concurrent.ScheduledExecutorService) heartbeatExecutor).scheduleAtFixedRate(
                this::sendHeartbeat,
                config.heartbeatIntervalMillis(),
                config.heartbeatIntervalMillis(),
                TimeUnit.MILLISECONDS
            );
        }
    }

    private void connect() {
        while (running.get() && !connected.get()) {
            try {
                doConnect();
                // Reset reconnect delay on successful connection
                currentReconnectDelay = config.reconnectDelayMillis();
                break;
            } catch (IOException e) {
                logger.warn("Failed to connect to RPC server at {}:{} - retrying in {}ms",
                    config.proxyHost(), config.proxyPort(), currentReconnectDelay, e);

                try {
                    Thread.sleep(currentReconnectDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // Exponential backoff
                currentReconnectDelay = Math.min(
                    (long) (currentReconnectDelay * config.reconnectMultiplier()),
                    config.maxReconnectDelayMillis()
                );
            }
        }
    }

    private void doConnect() throws IOException {
        logger.info("Connecting to RPC server at {}:{}...", config.proxyHost(), config.proxyPort());

        socket = new Socket(config.proxyHost(), config.proxyPort());
        socket.setSoTimeout((int) config.heartbeatTimeoutMillis());
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);

        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());

        connected.set(true);
        lastActivity.set(System.currentTimeMillis());

        // Send HELLO message
        sendHello();

        // Start reader thread
        readerThread = new Thread(this::runReaderLoop, "RapunzelLib-RPC-Reader");
        readerThread.setDaemon(true);
        readerThread.start();

        logger.info("Connected to RPC server at {}:{}", config.proxyHost(), config.proxyPort());
    }

    private void sendHello() {
        RpcProtocolMessage hello = RpcProtocolMessage.hello(config.serverName(), config.protocolVersion());
        sendProtocolMessage(hello);
    }

    private void sendHeartbeat() {
        if (!connected.get() || !running.get()) {
            return;
        }

        // Check if we've exceeded the heartbeat timeout
        long elapsed = System.currentTimeMillis() - lastActivity.get();
        if (elapsed > config.heartbeatTimeoutMillis()) {
            logger.warn("Heartbeat timeout - connection may be dead");
            handleDisconnect();
            return;
        }

        RpcProtocolMessage heartbeat = RpcProtocolMessage.heartbeat(config.serverName());
        if (sendProtocolMessage(heartbeat)) {
            logger.debug("Heartbeat sent");
        }
    }

    private void runReaderLoop() {
        while (running.get() && connected.get() && socket != null && !socket.isClosed()) {
            try {
                RpcProtocolMessage message = readMessage();
                if (message == null) {
                    continue;
                }

                handleMessage(message);
                lastActivity.set(System.currentTimeMillis());

            } catch (SocketTimeoutException e) {
                // Check heartbeat timeout
                long elapsed = System.currentTimeMillis() - lastActivity.get();
                if (elapsed > config.heartbeatTimeoutMillis()) {
                    logger.warn("Connection timeout - no activity for {}ms", elapsed);
                    break;
                }
            } catch (EOFException e) {
                logger.debug("Server closed connection (EOF)");
                break;
            } catch (SocketException e) {
                if (running.get()) {
                    logger.debug("Socket error: {}", e.getMessage());
                }
                break;
            } catch (IOException e) {
                if (running.get()) {
                    logger.warn("IO error in reader loop", e);
                }
                break;
            }
        }

        // Connection lost, attempt reconnection if still running
        if (running.get()) {
            handleDisconnect();
        }
    }

    private @Nullable RpcProtocolMessage readMessage() throws IOException {
        // Read 4-byte length prefix
        int length;
        try {
            length = input.readInt();
        } catch (EOFException e) {
            throw e;
        }

        if (length <= 0 || length > 10 * 1024 * 1024) { // Max 10MB
            logger.warn("Invalid message length: {}", length);
            return null;
        }

        // Read JSON payload
        byte[] payload = new byte[length];
        input.readFully(payload);

        String json = new String(payload, java.nio.charset.StandardCharsets.UTF_8);

        try {
            return gson.fromJson(json, RpcProtocolMessage.class);
        } catch (Exception e) {
            logger.warn("Failed to parse message JSON: {}", e.getMessage());
            return null;
        }
    }

    private void handleMessage(@NotNull RpcProtocolMessage message) {
        RpcProtocolMessage.Type type = message.getType();
        if (type == null) {
            logger.warn("Received message with null type");
            return;
        }

        switch (type) {
            case HELLO -> handleHello(message);
            case MESSAGE -> handleApplicationMessage(message);
            case HEARTBEAT -> handleHeartbeat(message);
            case DISCONNECT -> handleServerDisconnect(message);
            default -> logger.warn("Unknown message type: {}", type);
        }
    }

    private void handleHello(@NotNull RpcProtocolMessage message) {
        String name = message.getServerName();
        String version = message.getVersion();

        if (name != null && !name.isEmpty()) {
            this.proxyServerName = name;
        }
        this.identified.set(true);

        logger.info("Handshake complete with proxy '{}' (protocol version: {})",
            proxyServerName != null ? proxyServerName : "unknown",
            version != null ? version : "unknown");
    }

    private void handleApplicationMessage(@NotNull RpcProtocolMessage message) {
        String channel = message.getChannel();
        String data = message.getData();
        String sourceServer = message.getSourceServer();

        if (channel == null || data == null) {
            logger.warn("Invalid MESSAGE: missing channel or data");
            return;
        }

        // Default source to proxy if not specified
        if (sourceServer == null) {
            sourceServer = proxyServerName != null ? proxyServerName : NetworkDefaults.DEFAULT_PROXY_SERVER_NAME;
        }

        // Deliver to local listeners
        deliverToLocalListeners(channel, data, sourceServer);
    }

    private void handleHeartbeat(@NotNull RpcProtocolMessage message) {
        logger.debug("Heartbeat received from proxy");
        lastHeartbeat.set(System.currentTimeMillis());
    }

    private void handleServerDisconnect(@NotNull RpcProtocolMessage message) {
        logger.info("Server sent graceful disconnect");
        connected.set(false);
    }

    private void handleDisconnect() {
        if (!connected.compareAndSet(true, false)) {
            return; // Already disconnected
        }

        identified.set(false);
        logger.info("Disconnected from RPC server, will attempt reconnection");

        // Close current socket
        closeSocket();

        // Attempt reconnection if still running
        if (running.get()) {
            connect();
        }
    }

    private void closeSocket() {
        if (output != null) {
            try {
                output.close();
            } catch (IOException ignored) {
            }
            output = null;
        }
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
            }
            input = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }
    }

    private void deliverToLocalListeners(@NotNull String channel, @NotNull String data, @NotNull String sourceServer) {
        List<MessageListener> list = listeners.get(channel);
        if (list == null || list.isEmpty()) {
            return;
        }

        for (MessageListener listener : List.copyOf(list)) {
            try {
                listener.onMessage(channel, data, sourceServer);
            } catch (Exception e) {
                logger.warn("Listener error on channel {}", channel, e);
            }
        }
    }

    @Override
    public void sendToAll(@NotNull String channel, @NotNull String data) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(data, "data");

        if (!connected.get()) {
            logger.debug("Cannot sendToAll: not connected");
            return;
        }

        // Deliver to local listeners
        deliverToLocalListeners(channel, data, config.serverName());

        // Send to proxy for broadcast
        RpcProtocolMessage message = RpcProtocolMessage.message(
            channel, data, null, config.serverName()
        );
        sendProtocolMessage(message);
    }

    @Override
    public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(data, "data");

        if (!connected.get()) {
            logger.debug("Cannot sendToServer: not connected");
            return;
        }

        // If targeting this server, deliver locally
        if (serverName.equalsIgnoreCase(config.serverName())) {
            deliverToLocalListeners(channel, data, config.serverName());
            return;
        }

        // Send to proxy for routing
        RpcProtocolMessage message = RpcProtocolMessage.message(
            channel, data, serverName, config.serverName()
        );
        sendProtocolMessage(message);
    }

    @Override
    public void sendToProxy(@NotNull String channel, @NotNull String data) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(data, "data");

        if (!connected.get()) {
            logger.debug("Cannot sendToProxy: not connected");
            return;
        }

        // Send to proxy (target is null for proxy delivery)
        RpcProtocolMessage message = RpcProtocolMessage.message(
            channel, data, null, config.serverName()
        );
        sendProtocolMessage(message);
    }

    private synchronized boolean sendProtocolMessage(@NotNull RpcProtocolMessage message) {
        if (!connected.get() || output == null) {
            return false;
        }

        try {
            String json = gson.toJson(message);
            byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            output.writeInt(payload.length);
            output.write(payload);
            output.flush();

            lastActivity.set(System.currentTimeMillis());
            return true;
        } catch (IOException e) {
            logger.debug("Failed to send message: {}", e.getMessage());
            // Trigger reconnection
            handleDisconnect();
            return false;
        }
    }

    @Override
    public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(listener, "listener");

        listeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.debug("Registered listener for channel: {}", channel);
    }

    @Override
    public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(listener, "listener");

        List<MessageListener> list = listeners.get(channel);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                listeners.remove(channel);
            }
        }
        logger.debug("Unregistered listener for channel: {}", channel);
    }

    @Override
    public boolean isConnected() {
        return running.get() && connected.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public @NotNull String getServerName() {
        return config.serverName();
    }

    @Override
    public @NotNull String getProxyServerName() {
        return proxyServerName != null ? proxyServerName : NetworkDefaults.DEFAULT_PROXY_SERVER_NAME;
    }

    /**
     * Returns the client configuration.
     *
     * @return the configuration
     */
    public @NotNull RpcClientConfig getConfig() {
        return config;
    }

    /**
     * Returns true if the handshake with the proxy is complete.
     *
     * @return true if identified
     */
    public boolean isIdentified() {
        return identified.get();
    }

    /**
     * Returns the time of last activity (send or receive).
     *
     * @return timestamp in milliseconds
     */
    public long getLastActivity() {
        return lastActivity.get();
    }

    /**
     * Returns the time of last received heartbeat.
     *
     * @return timestamp in milliseconds
     */
    public long getLastHeartbeat() {
        return lastHeartbeat.get();
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return; // Already closing
        }

        connected.set(false);
        identified.set(false);
        logger.info("Shutting down RPC client...");

        // Send graceful disconnect
        if (output != null) {
            try {
                RpcProtocolMessage disconnect = RpcProtocolMessage.disconnect(config.serverName());
                String json = gson.toJson(disconnect);
                byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                output.writeInt(payload.length);
                output.write(payload);
                output.flush();
            } catch (IOException ignored) {
            }
        }

        // Shutdown heartbeat executor
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
            try {
                if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Close socket
        closeSocket();

        // Interrupt reader thread
        if (readerThread != null) {
            readerThread.interrupt();
        }

        logger.info("RPC client shutdown complete");
    }

    @Override
    public String toString() {
        return "RpcClientMessenger{" +
            "serverName='" + config.serverName() + '\'' +
            ", proxyHost='" + config.proxyHost() + '\'' +
            ", proxyPort=" + config.proxyPort() +
            ", connected=" + isConnected() +
            ", identified=" + isIdentified() +
            ", proxyServerName='" + proxyServerName + '\'' +
            '}';
    }
}
