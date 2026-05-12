package de.t14d3.rapunzellib.network.rpcserver;

import de.t14d3.rapunzellib.network.NetworkDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Configuration for the RPC server messenger.
 *
 * <p>Defines all configurable parameters for the RPC server including
 * network settings, identification, and connection management options.
 *
 * <p><strong>Configuration Parameters:</strong>
 * <ul>
 * <li><strong>port:</strong> TCP port for the server socket (default: 25566)</li>
 * <li><strong>serverName:</strong> Name identifying this proxy/server instance</li>
 * <li><strong>protocolVersion:</strong> Version string for protocol compatibility</li>
 * <li><strong>heartbeatIntervalMillis:</strong> Interval between heartbeats (default: 30000)</li>
 * <li><strong>heartbeatTimeoutMillis:</strong> Timeout for connection health (default: 60000)</li>
 * <li><strong>reconnectDelayMillis:</strong> Delay before reconnection attempts (default: 5000)</li>
 * <li><strong>maxClients:</strong> Maximum concurrent backend connections (default: 100)</li>
 * </ul>
 *
 * @since 1.0
 * @see RpcServerMessenger
 */
/**
 * Configuration settings for the RPC server.
 */
public class RpcServerConfig {

 private final String bindHost;
 private final int port;
 private final String serverName;
 private final String protocolVersion;
 private final long heartbeatIntervalMillis;
 private final long heartbeatTimeoutMillis;
 private final long reconnectDelayMillis;
 private final int maxClients;

 /**
 * Creates a new RPC server configuration with default values.
 *
 * @param serverName the name identifying this proxy/server instance
 */
 public RpcServerConfig(@NotNull String serverName) {
 this(NetworkDefaults.DEFAULT_RPC_BIND_HOST, NetworkDefaults.DEFAULT_RPC_PORT, serverName, "1.0", 30000L, 60000L, 5000L, 100);
 }

 /**
 * Creates a new RPC server configuration with custom port.
 *
 * @param port the TCP port for the server socket
 * @param serverName the name identifying this proxy/server instance
 */
 public RpcServerConfig(int port, @NotNull String serverName) {
  this(NetworkDefaults.DEFAULT_RPC_BIND_HOST, port, serverName, "1.0", 30000L, 60000L, 5000L, 100);
  }

 public RpcServerConfig(@NotNull String bindHost, int port, @NotNull String serverName) {
  this(bindHost, port, serverName, "1.0", 30000L, 60000L, 5000L, 100);
 }

 /**
 * Creates a new RPC server configuration with all parameters.
 *
 * @param port the TCP port for the server socket
 * @param serverName the name identifying this proxy/server instance
 * @param protocolVersion the protocol version string
 * @param heartbeatIntervalMillis interval between heartbeats in milliseconds
 * @param heartbeatTimeoutMillis timeout for connection health in milliseconds
 * @param reconnectDelayMillis delay before reconnection attempts in milliseconds
 * @param maxClients maximum concurrent backend connections
 */
 public RpcServerConfig(@NotNull String bindHost, int port, @NotNull String serverName, @NotNull String protocolVersion,
  long heartbeatIntervalMillis, long heartbeatTimeoutMillis,
  long reconnectDelayMillis, int maxClients) {
  this.bindHost = Objects.requireNonNull(bindHost, "bindHost");
  this.port = port;
 this.serverName = Objects.requireNonNull(serverName, "serverName");
 this.protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion");
 this.heartbeatIntervalMillis = heartbeatIntervalMillis;
 this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
 this.reconnectDelayMillis = reconnectDelayMillis;
  this.maxClients = maxClients;
  }

 public @NotNull String bindHost() {
  return bindHost;
 }

 /**
  * Returns the TCP port for the server socket.
 *
 * @return the port number (default: 25566)
 */
 public int port() {
 return port;
 }

 /**
 * Returns the name identifying this proxy/server instance.
 *
 * @return the server name
 */
 public @NotNull String serverName() {
 return serverName;
 }

 /**
 * Returns the protocol version string.
 *
 * @return the protocol version (default: "1.0")
 */
 public @NotNull String protocolVersion() {
 return protocolVersion;
 }

 /**
 * Returns the interval between heartbeat messages.
 *
 * @return heartbeat interval in milliseconds (default: 30000)
 */
 public long heartbeatIntervalMillis() {
 return heartbeatIntervalMillis;
 }

 /**
 * Returns the timeout for connection health detection.
 *
 * @return heartbeat timeout in milliseconds (default: 60000)
 */
 public long heartbeatTimeoutMillis() {
 return heartbeatTimeoutMillis;
 }

 /**
 * Returns the delay before reconnection attempts.
 *
 * @return reconnect delay in milliseconds (default: 5000)
 */
 public long reconnectDelayMillis() {
 return reconnectDelayMillis;
 }

 /**
 * Returns the maximum number of concurrent backend connections.
 *
 * @return maximum clients (default: 100)
 */
 public int maxClients() {
 return maxClients;
 }

 /**
 * Creates a builder for fluent configuration.
 *
 * @param serverName the server name
 * @return a new configuration builder
 */
 public static Builder builder(@NotNull String serverName) {
 return new Builder(serverName);
 }

 /**
 * Builder for RpcServerConfig.
 */
 public static class Builder {
 private String bindHost = NetworkDefaults.DEFAULT_RPC_BIND_HOST;
 private int port = NetworkDefaults.DEFAULT_RPC_PORT;
 private final String serverName;
 private String protocolVersion = "1.0";
 private long heartbeatIntervalMillis = 30000L;
 private long heartbeatTimeoutMillis = 60000L;
 private long reconnectDelayMillis = 5000L;
 private int maxClients = 100;

  private Builder(@NotNull String serverName) {
  this.serverName = Objects.requireNonNull(serverName, "serverName");
  }

  public Builder bindHost(@NotNull String bindHost) {
  this.bindHost = Objects.requireNonNull(bindHost, "bindHost");
  return this;
  }

  public Builder port(int port) {
  this.port = port;
 return this;
 }

 public Builder protocolVersion(@NotNull String version) {
 this.protocolVersion = Objects.requireNonNull(version, "version");
 return this;
 }

 public Builder heartbeatIntervalMillis(long millis) {
 this.heartbeatIntervalMillis = millis;
 return this;
 }

 public Builder heartbeatTimeoutMillis(long millis) {
 this.heartbeatTimeoutMillis = millis;
 return this;
 }

 public Builder reconnectDelayMillis(long millis) {
 this.reconnectDelayMillis = millis;
 return this;
 }

 public Builder maxClients(int max) {
 this.maxClients = max;
 return this;
 }

  public RpcServerConfig build() {
  return new RpcServerConfig(bindHost, port, serverName, protocolVersion,
  heartbeatIntervalMillis, heartbeatTimeoutMillis,
  reconnectDelayMillis, maxClients);
  }
 }
}
