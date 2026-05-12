package de.t14d3.rapunzellib.network.rpcserver;

import de.t14d3.rapunzellib.network.NetworkDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Configuration for the RPC client messenger.
 *
 * <p>Defines all configurable parameters for the RPC client including
 * network settings, identification, and connection management options.
 *
 * <p><strong>Configuration Parameters:</strong>
 * <ul>
 * <li><strong>proxyHost:</strong> Hostname/IP of the proxy RPC server (default: "127.0.0.1")</li>
 * <li><strong>proxyPort:</strong> TCP port of the proxy RPC server (default: 25566)</li>
 * <li><strong>serverName:</strong> Name identifying this backend server instance</li>
 * <li><strong>protocolVersion:</strong> Version string for protocol compatibility</li>
 * <li><strong>heartbeatIntervalMillis:</strong> Interval between heartbeats (default: 30000)</li>
 * <li><strong>heartbeatTimeoutMillis:</strong> Timeout for connection health (default: 60000)</li>
 * <li><strong>reconnectDelayMillis:</strong> Initial delay before reconnection attempts (default: 5000)</li>
 * <li><strong>maxReconnectDelayMillis:</strong> Maximum reconnection delay with exponential backoff (default: 60000)</li>
 * <li><strong>reconnectMultiplier:</strong> Exponential backoff multiplier (default: 2.0)</li>
 * </ul>
 *
 * @since 1.0
 * @see RpcClientMessenger
 */
public class RpcClientConfig {

 private final String proxyHost;
 private final int proxyPort;
 private final String serverName;
 private final String protocolVersion;
 private final long heartbeatIntervalMillis;
 private final long heartbeatTimeoutMillis;
 private final long reconnectDelayMillis;
 private final long maxReconnectDelayMillis;
 private final double reconnectMultiplier;

 /**
 * Creates a new RPC client configuration with default values.
 *
 * @param serverName the name identifying this backend server instance
 */
 public RpcClientConfig(@NotNull String serverName) {
 this(NetworkDefaults.DEFAULT_RPC_HOST, NetworkDefaults.DEFAULT_RPC_PORT, serverName, "1.0", 30000L, 60000L, 5000L, 60000L, 2.0);
 }

 /**
 * Creates a new RPC client configuration with custom proxy address.
 *
 * @param proxyHost the hostname/IP of the proxy RPC server
 * @param proxyPort the TCP port of the proxy RPC server
 * @param serverName the name identifying this backend server instance
 */
 public RpcClientConfig(@NotNull String proxyHost, int proxyPort, @NotNull String serverName) {
 this(proxyHost, proxyPort, serverName, "1.0", 30000L, 60000L, 5000L, 60000L, 2.0);
 }

 /**
 * Creates a new RPC client configuration with all parameters.
 *
 * @param proxyHost the hostname/IP of the proxy RPC server
 * @param proxyPort the TCP port of the proxy RPC server
 * @param serverName the name identifying this backend server instance
 * @param protocolVersion the protocol version string
 * @param heartbeatIntervalMillis interval between heartbeats in milliseconds
 * @param heartbeatTimeoutMillis timeout for connection health in milliseconds
 * @param reconnectDelayMillis initial delay before reconnection attempts
 * @param maxReconnectDelayMillis maximum reconnection delay with exponential backoff
 * @param reconnectMultiplier exponential backoff multiplier
 */
 public RpcClientConfig(@NotNull String proxyHost, int proxyPort, @NotNull String serverName,
 @NotNull String protocolVersion, long heartbeatIntervalMillis,
 long heartbeatTimeoutMillis, long reconnectDelayMillis,
 long maxReconnectDelayMillis, double reconnectMultiplier) {
 this.proxyHost = Objects.requireNonNull(proxyHost, "proxyHost");
 this.proxyPort = proxyPort;
 this.serverName = Objects.requireNonNull(serverName, "serverName");
 this.protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion");
 this.heartbeatIntervalMillis = heartbeatIntervalMillis;
 this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
 this.reconnectDelayMillis = reconnectDelayMillis;
 this.maxReconnectDelayMillis = maxReconnectDelayMillis;
 this.reconnectMultiplier = reconnectMultiplier;
 }

 /**
 * Returns the hostname/IP of the proxy RPC server.
 *
  * @return the proxy host (default: "127.0.0.1")
 */
 public @NotNull String proxyHost() {
 return proxyHost;
 }

 /**
 * Returns the TCP port of the proxy RPC server.
 *
 * @return the proxy port (default: 25566)
 */
 public int proxyPort() {
 return proxyPort;
 }

 /**
 * Returns the name identifying this backend server instance.
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
 * Returns the initial delay before reconnection attempts.
 *
 * @return reconnect delay in milliseconds (default: 5000)
 */
 public long reconnectDelayMillis() {
 return reconnectDelayMillis;
 }

 /**
 * Returns the maximum reconnection delay with exponential backoff.
 *
 * @return maximum reconnect delay in milliseconds (default: 60000)
 */
 public long maxReconnectDelayMillis() {
 return maxReconnectDelayMillis;
 }

 /**
 * Returns the exponential backoff multiplier.
 *
 * @return reconnect multiplier (default: 2.0)
 */
 public double reconnectMultiplier() {
 return reconnectMultiplier;
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
 * Builder for RpcClientConfig.
 */
 public static class Builder {
  private String proxyHost = NetworkDefaults.DEFAULT_RPC_HOST;
  private int proxyPort = NetworkDefaults.DEFAULT_RPC_PORT;
 private final String serverName;
 private String protocolVersion = "1.0";
 private long heartbeatIntervalMillis = 30000L;
 private long heartbeatTimeoutMillis = 60000L;
 private long reconnectDelayMillis = 5000L;
 private long maxReconnectDelayMillis = 60000L;
 private double reconnectMultiplier = 2.0;

 private Builder(@NotNull String serverName) {
 this.serverName = Objects.requireNonNull(serverName, "serverName");
 }

    /**
     * Sets the proxy host address.
     *
     * @param host the proxy hostname or IP
     * @return this builder
     */
    public Builder proxyHost(@NotNull String host) {
 this.proxyHost = Objects.requireNonNull(host, "host");
 return this;
 }

    /**
     * Sets the proxy port.
     *
     * @param port the proxy TCP port
     * @return this builder
     */
    public Builder proxyPort(int port) {
 this.proxyPort = port;
 return this;
 }

    /**
     * Sets the protocol version string.
     *
     * @param version the protocol version
     * @return this builder
     */
    public Builder protocolVersion(@NotNull String version) {
 this.protocolVersion = Objects.requireNonNull(version, "version");
 return this;
 }

    /**
     * Sets the heartbeat interval.
     *
     * @param millis interval in milliseconds
     * @return this builder
     */
    public Builder heartbeatIntervalMillis(long millis) {
 this.heartbeatIntervalMillis = millis;
 return this;
 }

    /**
     * Sets the heartbeat timeout.
     *
     * @param millis timeout in milliseconds
     * @return this builder
     */
    public Builder heartbeatTimeoutMillis(long millis) {
 this.heartbeatTimeoutMillis = millis;
 return this;
 }

    /**
     * Sets the initial reconnection delay.
     *
     * @param millis delay in milliseconds
     * @return this builder
     */
    public Builder reconnectDelayMillis(long millis) {
 this.reconnectDelayMillis = millis;
 return this;
 }

    /**
     * Sets the maximum reconnection delay for exponential backoff.
     *
     * @param millis max delay in milliseconds
     * @return this builder
     */
    public Builder maxReconnectDelayMillis(long millis) {
 this.maxReconnectDelayMillis = millis;
 return this;
 }

    /**
     * Sets the exponential backoff reconnect multiplier.
     *
     * @param multiplier the multiplier value
     * @return this builder
     */
    public Builder reconnectMultiplier(double multiplier) {
 this.reconnectMultiplier = multiplier;
 return this;
 }

 public RpcClientConfig build() {
 return new RpcClientConfig(proxyHost, proxyPort, serverName, protocolVersion,
 heartbeatIntervalMillis, heartbeatTimeoutMillis,
 reconnectDelayMillis, maxReconnectDelayMillis, reconnectMultiplier);
 }
 }
}
