package de.t14d3.rapunzellib.network.rpcserver;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RPC Server Messenger implementation for RapunzelLib.
 *
 * <p>Creates an RPC-like server where the proxy (Velocity) acts as the master/server
 * and backend Minecraft servers connect as clients. Uses TCP sockets with a custom
 * protocol for message exchange.
 *
 * <p><strong>Architecture:</strong>
 * <ul>
 * <li>Proxy starts a {@link ServerSocket} on a configurable port (default: 25566)</li>
 * <li>Backend servers connect via TCP and identify themselves with HELLO message</li>
 * <li>Protocol: JSON-based messages with 4-byte length prefix for framing</li>
 * <li>Each connection is handled by a {@link BackendClientHandler}</li>
 * <li>Thread-safe message routing using concurrent collections</li>
 * </ul>
 *
 * <p><strong>Protocol Message Types:</strong>
 * <ul>
 * <li><strong>HELLO:</strong> Backend identifies itself (serverName, version)</li>
 * <li><strong>MESSAGE:</strong> Actual message envelope for application data</li>
 * <li><strong>HEARTBEAT:</strong> Keepalive for connection health</li>
 * <li><strong>DISCONNECT:</strong> Graceful disconnect notification</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * RpcServerConfig config = RpcServerConfig.builder("proxy")
 * .port(25566)
 * .maxClients(100)
 * .build();
 *
 * try (RpcServerMessenger messenger = new RpcServerMessenger(config)) {
 * messenger.registerListener("mychannel", (channel, data, source) -> {
 * System.out.println("Received from " + source + ": " + data);
 * });
 *
 * messenger.sendToAll("mychannel", "{\"message\":\"hello\"}");
 * messenger.sendToServer("mychannel", "lobby", "{\"message\":\"direct\"}");
 * messenger.sendToProxy("mychannel", "{\"message\":\"to proxy\"}");
 * }
 * }</pre>
 *
 * @since 1.0
 * @see Messenger
 * @see RpcServerConfig
 * @see BackendClientHandler
 * @see RpcProtocolMessage
 */
public class RpcServerMessenger implements Messenger, AutoCloseable {

 private final RpcServerConfig config;
 private final Logger logger;
 private final Gson gson = JsonCodecs.gson();

 private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();
 private final Map<String, BackendClientHandler> clients = new ConcurrentHashMap<>();

 private volatile ServerSocket serverSocket;
 private volatile ExecutorService clientExecutor;
 private volatile Thread acceptThread;

 private final AtomicBoolean running = new AtomicBoolean(false);
 private final AtomicBoolean connected = new AtomicBoolean(false);

 /**
 * Creates a new RPC server messenger with the specified configuration.
 *
 * @param config the RPC server configuration
 * @throws IllegalArgumentException if config is null
 */
 public RpcServerMessenger(@NotNull RpcServerConfig config) {
 this(config, LoggerFactory.getLogger(RpcServerMessenger.class));
 }

 /**
 * Creates a new RPC server messenger with custom logger.
 *
 * @param config the RPC server configuration
 * @param logger the logger instance
 * @throws IllegalArgumentException if config or logger is null
 */
 public RpcServerMessenger(@NotNull RpcServerConfig config, @NotNull Logger logger) {
 this.config = Objects.requireNonNull(config, "config");
 this.logger = Objects.requireNonNull(logger, "logger");

 startServer();
 }

 /**
 * Starts the server socket and accept thread.
 */
 private void startServer() {
 if (running.compareAndSet(false, true)) {
 try {
  serverSocket = new ServerSocket();
  serverSocket.bind(new InetSocketAddress(config.bindHost(), config.port()));
  serverSocket.setReuseAddress(true);

 clientExecutor = Executors.newCachedThreadPool(r -> {
 Thread t = new Thread(r, "RapunzelLib-RPC-Client-" + r.hashCode());
 t.setDaemon(true);
 return t;
 });

 acceptThread = new Thread(this::runAcceptLoop, "RapunzelLib-RPC-Accept");
 acceptThread.setDaemon(true);
 acceptThread.start();

 connected.set(true);
  logger.info("RPC Server started on {}:{} (max clients: {})",
  config.bindHost(), config.port(), config.maxClients());

 } catch (IOException e) {
 running.set(false);
 connected.set(false);
 throw new RuntimeException("Failed to start RPC server on port " + config.port(), e);
 }
 }
 }

 /**
 * Main accept loop for incoming connections.
 */
 private void runAcceptLoop() {
 while (running.get() && serverSocket != null && !serverSocket.isClosed()) {
 try {
 Socket clientSocket = serverSocket.accept();

 // Check max clients
 if (clients.size() >= config.maxClients()) {
 logger.warn("Max clients ({}) reached, rejecting connection from {}",
 config.maxClients(), clientSocket.getInetAddress());
 try {
 clientSocket.close();
 } catch (IOException ignored) {
 }
 continue;
 }

 logger.debug("Accepted connection from {}", clientSocket.getInetAddress());

 // Create handler and submit to executor
 BackendClientHandler handler = new BackendClientHandler(
 clientSocket, gson, logger, config, listeners, clients, config.serverName()
 );
 clientExecutor.submit(handler);

 } catch (SocketException e) {
 if (running.get()) {
 logger.debug("Socket accept error (server may be closing): {}", e.getMessage());
 }
 } catch (IOException e) {
 if (running.get()) {
 logger.warn("Error accepting client connection", e);
 }
 }
 }

 connected.set(false);
 logger.debug("RPC server accept loop ended");
 }

 @Override
 public void sendToAll(@NotNull String channel, @NotNull String data) {
 Objects.requireNonNull(channel, "channel");
 Objects.requireNonNull(data, "data");

 if (!running.get()) {
 logger.debug("Cannot sendToAll: server not running");
 return;
 }

 // Deliver to local listeners
 deliverToLocalListeners(channel, data, config.serverName());

 // Broadcast to all connected backends
 RpcProtocolMessage message = RpcProtocolMessage.message(
 channel, data, null, config.serverName()
 );

 synchronized (clients) {
 for (BackendClientHandler client : clients.values()) {
 client.sendMessage(message);
 }
 }
 }

 @Override
 public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
 Objects.requireNonNull(channel, "channel");
 Objects.requireNonNull(serverName, "serverName");
 Objects.requireNonNull(data, "data");

 if (!running.get()) {
 logger.debug("Cannot sendToServer: server not running");
 return;
 }

 // If targeting this proxy, deliver locally
 if (serverName.equalsIgnoreCase(config.serverName())) {
 deliverToLocalListeners(channel, data, config.serverName());
 return;
 }

 // Send to specific backend
 BackendClientHandler client = clients.get(serverName);
 if (client != null) {
 client.sendToServer(channel, data, config.serverName());
 } else {
 logger.debug("Target server '{}' not connected", serverName);
 }
 }

 @Override
 public void sendToProxy(@NotNull String channel, @NotNull String data) {
 Objects.requireNonNull(channel, "channel");
 Objects.requireNonNull(data, "data");

 // As the proxy, we just deliver locally
 deliverToLocalListeners(channel, data, config.serverName());
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
 return running.get() && connected.get() && serverSocket != null && !serverSocket.isClosed();
 }

 @Override
 public @NotNull String getServerName() {
 return config.serverName();
 }

 @Override
 public @NotNull String getProxyServerName() {
 // As the proxy, we return our own name
 return config.serverName();
 }

 /**
 * Returns the number of currently connected backend servers.
 *
 * @return the number of connected clients
 */
 public int getConnectedClientCount() {
 return clients.size();
 }

 /**
 * Returns the names of all connected backend servers.
 *
 * @return array of connected server names
 */
 public @NotNull String[] getConnectedServerNames() {
 return clients.keySet().toArray(new String[0]);
 }

 /**
 * Checks if a specific backend server is connected.
 *
 * @param serverName the server name to check
 * @return true if connected
 */
 public boolean isServerConnected(@NotNull String serverName) {
 return clients.containsKey(serverName);
 }

 /**
 * Delivers a message to local registered listeners.
 */
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

 /**
 * Closes all client connections.
 */
 private void closeAllClients() {
 synchronized (clients) {
 for (BackendClientHandler client : clients.values()) {
 try {
 client.close();
 } catch (Exception ignored) {
 }
 }
 clients.clear();
 }
 }

 @Override
 public void close() {
 if (!running.compareAndSet(true, false)) {
 return; // Already closing
 }

 connected.set(false);
 logger.info("Shutting down RPC server...");

 // Close all client connections
 closeAllClients();

 // Shutdown executor
 if (clientExecutor != null) {
 clientExecutor.shutdown();
 try {
 if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
 clientExecutor.shutdownNow();
 }
 } catch (InterruptedException e) {
 clientExecutor.shutdownNow();
 Thread.currentThread().interrupt();
 }
 }

 // Close server socket
 if (serverSocket != null && !serverSocket.isClosed()) {
 try {
 serverSocket.close();
 } catch (IOException e) {
 logger.warn("Error closing server socket", e);
 }
 }

 // Interrupt accept thread
 if (acceptThread != null) {
 acceptThread.interrupt();
 }

 logger.info("RPC server shutdown complete");
 }

 /**
 * Returns the server configuration.
 *
 * @return the configuration
 */
 public @NotNull RpcServerConfig getConfig() {
 return config;
 }

 @Override
 public String toString() {
 return "RpcServerMessenger{" +
 "serverName='" + config.serverName() + '\'' +
 ", port=" + config.port() +
 ", connected=" + isConnected() +
 ", clients=" + getConnectedClientCount() +
 '}';
 }
}
