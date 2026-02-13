package de.t14d3.rapunzellib.network.rpcserver;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles communication with a single backend server client.
 *
 * <p>Manages the socket connection, message framing, protocol handling,
 * and message routing for an individual backend server connection.
 *
 * <p><strong>Protocol:</strong>
 * <pre>
 * [4 bytes: length][JSON payload]
 * </pre>
 *
 * <p><strong>Message Flow:</strong>
 * <ol>
 * <li>Client connects and sends HELLO with server identification</li>
 * <li>Server acknowledges and registers the client</li>
 * <li>Bi-directional MESSAGE exchange begins</li>
 * <li>Periodic HEARTBEAT messages maintain connection health</li>
 * <li>DISCONNECT message or socket close terminates connection</li>
 * </ol>
 *
 * @since 1.0
 * @see RpcServerMessenger
 * @see RpcProtocolMessage
 */
public class BackendClientHandler implements Runnable, AutoCloseable {

 private final Socket socket;
 private final DataInputStream input;
 private final DataOutputStream output;
 private final Gson gson;
 private final Logger logger;
 private final RpcServerConfig config;

 private final AtomicBoolean running = new AtomicBoolean(true);
 private final AtomicBoolean identified = new AtomicBoolean(false);
 private volatile String serverName;
 private final AtomicLong lastHeartbeat = new AtomicLong(System.currentTimeMillis());

 private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners;
 private final Map<String, BackendClientHandler> clients;
 private final String localServerName;

 /**
 * Creates a new backend client handler.
 *
 * @param socket the client socket
 * @param gson the Gson instance for JSON serialization
 * @param logger the logger instance
 * @param config the RPC server configuration
 * @param listeners the shared listener registry
 * * @param clients the shared client handler registry
 * @param localServerName the name of this proxy server
 * @throws IOException if streams cannot be created
 */
 public BackendClientHandler(@NotNull Socket socket, @NotNull Gson gson, @NotNull Logger logger,
 @NotNull RpcServerConfig config,
 @NotNull Map<String, CopyOnWriteArrayList<MessageListener>> listeners,
 @NotNull Map<String, BackendClientHandler> clients,
 @NotNull String localServerName) throws IOException {
 this.socket = socket;
 this.gson = gson;
 this.logger = logger;
 this.config = config;
 this.listeners = listeners;
 this.clients = clients;
 this.localServerName = localServerName;

 this.socket.setSoTimeout((int) config.heartbeatTimeoutMillis());
 this.socket.setKeepAlive(true);
 this.socket.setTcpNoDelay(true);

 this.input = new DataInputStream(socket.getInputStream());
 this.output = new DataOutputStream(socket.getOutputStream());
 }

 @Override
 public void run() {
 try {
 while (running.get() && !socket.isClosed()) {
 try {
 RpcProtocolMessage message = readMessage();
 if (message == null) {
 continue;
 }

 handleMessage(message);

 // Update last activity time
 lastHeartbeat.set(System.currentTimeMillis());

 } catch (SocketTimeoutException e) {
 // Check if we've exceeded the heartbeat timeout
 long elapsed = System.currentTimeMillis() - lastHeartbeat.get();
 if (elapsed > config.heartbeatTimeoutMillis()) {
 logger.warn("Heartbeat timeout for client {}", serverName != null ? serverName : "unidentified");
 break;
 }
 } catch (EOFException e) {
 logger.debug("Client {} disconnected (EOF)", serverName != null ? serverName : "unidentified");
 break;
 } catch (SocketException e) {
 if (running.get()) {
 logger.debug("Socket error for client {}: {}", serverName != null ? serverName : "unidentified", e.getMessage());
 }
 break;
 }
 }
 } catch (IOException e) {
 if (running.get()) {
 logger.warn("IO error handling client {}", serverName != null ? serverName : "unidentified", e);
 }
 } finally {
 close();
 }
 }

 /**
 * Reads a framed message from the input stream.
 *
 * @return the parsed protocol message, or null if invalid
 * @throws IOException if reading fails
 */
 private @Nullable RpcProtocolMessage readMessage() throws IOException {
 // Read 4-byte length prefix
 int length;
 try {
 length = input.readInt();
 } catch (EOFException e) {
 throw e; // Re-throw to signal disconnect
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

 /**
 * Handles an incoming protocol message.
 *
 * @param message the message to handle
 */
 private void handleMessage(@NotNull RpcProtocolMessage message) {
 RpcProtocolMessage.Type type = message.getType();
 if (type == null) {
 logger.warn("Received message with null type from {}", serverName);
 return;
 }

 switch (type) {
 case HELLO -> handleHello(message);
 case MESSAGE -> handleApplicationMessage(message);
 case HEARTBEAT -> handleHeartbeat(message);
 case DISCONNECT -> handleDisconnect(message);
 default -> logger.warn("Unknown message type: {}", type);
 }
 }

 /**
 * Handles HELLO message from backend.
 */
 private void handleHello(@NotNull RpcProtocolMessage message) {
 String name = message.getServerName();
 String version = message.getVersion();

 if (name == null || name.isEmpty()) {
 logger.warn("HELLO message missing server name");
 close();
 return;
 }

 // Check for duplicate connections
 synchronized (clients) {
 BackendClientHandler existing = clients.get(name);
 if (existing != null && existing != this) {
 logger.warn("Duplicate connection from server '{}', closing old connection", name);
 existing.close();
 }
 clients.put(name, this);
 }

 this.serverName = name;
 this.identified.set(true);

 logger.info("Backend server '{}' connected (protocol version: {})", name, version != null ? version : "unknown");

 // Send acknowledgment
 sendMessage(RpcProtocolMessage.hello(localServerName, config.protocolVersion()));
 }

 /**
 * Handles application MESSAGE envelope.
 */
 private void handleApplicationMessage(@NotNull RpcProtocolMessage message) {
 if (!identified.get()) {
 logger.warn("Received MESSAGE before HELLO from {}", socket.getInetAddress());
 return;
 }

 String channel = message.getChannel();
 String data = message.getData();
 String targetServer = message.getTargetServer();
 String sourceServer = message.getSourceServer();

 if (channel == null || data == null) {
 logger.warn("Invalid MESSAGE from {}: missing channel or data", serverName);
 return;
 }

 // Default source to the connected server
 if (sourceServer == null) {
 sourceServer = serverName;
 }

 // Route the message
 routeMessage(channel, data, targetServer, sourceServer);
 }

 /**
 * Routes a message to appropriate destinations.
 */
 private void routeMessage(@NotNull String channel, @NotNull String data,
 @Nullable String targetServer, @NotNull String sourceServer) {
 // Deliver to local listeners
 deliverToLocalListeners(channel, data, sourceServer);

 // Route to target server if specified
 if (targetServer != null && !targetServer.equalsIgnoreCase(localServerName)) {
 BackendClientHandler target = clients.get(targetServer);
 if (target != null) {
 target.sendToServer(channel, data, sourceServer);
 }
 } else if (targetServer == null) {
 // Broadcast to all other backends (except source)
 broadcastToBackends(channel, data, sourceServer);
 }
 }

 /**
 * Delivers message to local registered listeners.
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
 * Broadcasts message to all connected backends except the source.
 */
 private void broadcastToBackends(@NotNull String channel, @NotNull String data, @NotNull String sourceServer) {
 RpcProtocolMessage message = RpcProtocolMessage.message(channel, data, null, sourceServer);

 synchronized (clients) {
 for (Map.Entry<String, BackendClientHandler> entry : clients.entrySet()) {
 if (!entry.getKey().equalsIgnoreCase(sourceServer)) {
 entry.getValue().sendMessage(message);
 }
 }
 }
 }

 /**
 * Handles HEARTBEAT message.
 */
 private void handleHeartbeat(@NotNull RpcProtocolMessage message) {
 // Just update the timestamp - already done in run()
 logger.debug("Heartbeat received from {}", serverName);
 }

 /**
 * Handles DISCONNECT message.
 */
 private void handleDisconnect(@NotNull RpcProtocolMessage message) {
 logger.info("Client {} sent graceful disconnect", serverName);
 running.set(false);
 }

 /**
 * Sends a message to this backend server.
 *
 * @param channel the message channel
 * @param data the message data
 * @param sourceServer the source server name
 * @return true if sent successfully
 */
 public boolean sendToServer(@NotNull String channel, @NotNull String data, @NotNull String sourceServer) {
 RpcProtocolMessage message = RpcProtocolMessage.message(channel, data, null, sourceServer);
 return sendMessage(message);
 }

 /**
 * Sends a protocol message to this client.
 *
 * @param message the message to send
 * @return true if sent successfully
 */
 public synchronized boolean sendMessage(@NotNull RpcProtocolMessage message) {
 if (socket.isClosed() || !running.get()) {
 return false;
 }

 try {
 String json = gson.toJson(message);
 byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

 output.writeInt(payload.length);
 output.write(payload);
 output.flush();

 return true;
 } catch (IOException e) {
 logger.debug("Failed to send message to {}: {}", serverName, e.getMessage());
 close();
 return false;
 }
 }

 /**
 * Returns the server name if identified.
 *
 * @return the server name, or null if not yet identified
 */
 public @Nullable String getServerName() {
 return serverName;
 }

 /**
 * Returns true if this client has completed the HELLO handshake.
 *
 * @return true if identified
 */
 public boolean isIdentified() {
 return identified.get();
 }

 /**
 * Returns true if the connection is active.
 *
 * @return true if connected and running
 */
 public boolean isConnected() {
 return running.get() && !socket.isClosed() && socket.isConnected();
 }

 /**
 * Returns the time of last heartbeat/activity.
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

 // Remove from client registry
 if (serverName != null) {
 synchronized (clients) {
 clients.remove(serverName, this);
 }
 logger.info("Backend server '{}' disconnected", serverName);
 }

 // Close streams and socket
 try {
 output.close();
 } catch (IOException ignored) {
 }
 try {
 input.close();
 } catch (IOException ignored) {
 }
 try {
 socket.close();
 } catch (IOException ignored) {
 }
 }

 @Override
 public String toString() {
 return "BackendClientHandler{" +
 "serverName='" + serverName + '\'' +
 ", identified=" + identified.get() +
 ", connected=" + isConnected() +
 '}';
 }
}
