package de.t14d3.rapunzellib.network.redis;

import de.t14d3.rapunzellib.network.NetworkDefaults;

/**
 * Configuration for Redis pub/sub messenger transport.
 *
 * <p>Defines connection parameters, authentication, and channel settings for
 * Redis-based network communication between servers.
 */
@SuppressWarnings("SameParameterValue")
public final class RedisPubSubConfig {
    private final String host;
    private final int port;
    private final boolean ssl;
    private final String username;
    private final String password;
    private final String transportChannel;
    private final String serverName;
    private final String proxyServerName;
    private final int connectTimeoutMillis;
    private final int socketTimeoutMillis;
    private final long reconnectDelayMillis;
    private final String clientName;

    private RedisPubSubConfig(
        String host,
        int port,
        boolean ssl,
        String username,
        String password,
        String transportChannel,
        String serverName,
        String proxyServerName,
        int connectTimeoutMillis,
        int socketTimeoutMillis,
        long reconnectDelayMillis,
        String clientName
    ) {
        this.host = requireNonBlank(host, "host");
        this.port = requirePort(port);
        this.ssl = ssl;
        this.username = emptyToNull(username);
        this.password = emptyToNull(password);
        this.transportChannel = requireNonBlank(transportChannel, "transportChannel");
        this.serverName = requireNonBlank(serverName, "serverName");
        this.proxyServerName = requireNonBlank(proxyServerName, "proxyServerName");
        this.connectTimeoutMillis = requirePositive(connectTimeoutMillis, "connectTimeoutMillis");
        this.socketTimeoutMillis = requireNonNegative(socketTimeoutMillis, "socketTimeoutMillis");
        this.reconnectDelayMillis = requireNonNegative(reconnectDelayMillis, "reconnectDelayMillis");
        this.clientName = emptyToNull(clientName);
    }

    /**
     * Returns the Redis host address.
     *
     * @return the host
     */
    public String host() {
        return host;
    }

    /**
     * Returns the Redis port.
     *
     * @return the port
     */
    public int port() {
        return port;
    }

    /**
     * Returns whether SSL/TLS is enabled for the Redis connection.
     *
     * @return true if SSL is enabled
     */
    public boolean ssl() {
        return ssl;
    }

    /**
     * Returns the Redis username for authentication.
     *
     * @return the username, or null if not set
     */
    public String username() {
        return username;
    }

    /**
     * Returns the Redis password for authentication.
     *
     * @return the password, or null if not set
     */
    public String password() {
        return password;
    }

    /**
     * Returns the transport channel name used for pub/sub messages.
     *
     * @return the transport channel
     */
    public String transportChannel() {
        return transportChannel;
    }

    /**
     * Returns the server name identifying this instance.
     *
     * @return the server name
     */
    public String serverName() {
        return serverName;
    }

    /**
     * Returns the proxy server name.
     *
     * @return the proxy server name
     */
    public String proxyServerName() {
        return proxyServerName;
    }

    /**
     * Returns the Redis connection timeout in milliseconds.
     *
     * @return the connect timeout
     */
    public int connectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    /**
     * Returns the Redis socket timeout in milliseconds.
     *
     * @return the socket timeout
     */
    public int socketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    /**
     * Returns the delay before Redis reconnection attempts in milliseconds.
     *
     * @return the reconnect delay
     */
    public long reconnectDelayMillis() {
        return reconnectDelayMillis;
    }

    /**
     * Returns the Redis client name.
     *
     * @return the client name, or null if not set
     */
    public String clientName() {
        return clientName;
    }

    /**
     * Creates a builder for fluent configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "RedisPubSubConfig{" +
            "host='" + host + '\'' +
            ", port=" + port +
            ", ssl=" + ssl +
            ", username=" + (username != null ? "'" + username + "'" : "null") +
            ", password=" + (password != null ? "<redacted>" : "null") +
            ", transportChannel='" + transportChannel + '\'' +
            ", serverName='" + serverName + '\'' +
            ", proxyServerName='" + proxyServerName + '\'' +
            ", connectTimeoutMillis=" + connectTimeoutMillis +
            ", socketTimeoutMillis=" + socketTimeoutMillis +
            ", reconnectDelayMillis=" + reconnectDelayMillis +
            ", clientName=" + (clientName != null ? "'" + clientName + "'" : "null") +
            '}';
    }

    /**
     * Builder for {@link RedisPubSubConfig}.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        private String host = NetworkDefaults.DEFAULT_RPC_HOST;
        private int port = NetworkDefaults.DEFAULT_REDIS_PORT;
        private boolean ssl;
        private String username;
        private String password;
        private String transportChannel = "rapunzellib:bridge";
        private String serverName;
        private String proxyServerName = NetworkDefaults.DEFAULT_PROXY_SERVER_NAME;
        private int connectTimeoutMillis = NetworkDefaults.DEFAULT_REDIS_CONNECT_TIMEOUT_MILLIS;
        private int socketTimeoutMillis = NetworkDefaults.DEFAULT_REDIS_SOCKET_TIMEOUT_MILLIS;
        private long reconnectDelayMillis = NetworkDefaults.DEFAULT_REDIS_RECONNECT_DELAY_MILLIS;
        private String clientName;

        private Builder() {
        }

        /**
         * Sets the Redis host.
         *
         * @param host the hostname or IP
         * @return this builder
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * Sets the Redis port.
         *
         * @param port the port number
         * @return this builder
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets whether SSL/TLS is enabled.
         *
         * @param ssl true to enable SSL
         * @return this builder
         */
        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        /**
         * Sets the Redis username.
         *
         * @param username the username
         * @return this builder
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Sets the Redis password.
         *
         * @param password the password
         * @return this builder
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the transport channel name.
         *
         * @param transportChannel the channel name
         * @return this builder
         */
        public Builder transportChannel(String transportChannel) {
            this.transportChannel = transportChannel;
            return this;
        }

        /**
         * Sets the server name.
         *
         * @param serverName the server name
         * @return this builder
         */
        public Builder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        /**
         * Sets the proxy server name.
         *
         * @param proxyServerName the proxy server name
         * @return this builder
         */
        public Builder proxyServerName(String proxyServerName) {
            this.proxyServerName = proxyServerName;
            return this;
        }

        /**
         * Sets the connection timeout in milliseconds.
         *
         * @param connectTimeoutMillis the connect timeout
         * @return this builder
         */
        public Builder connectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        /**
         * Sets the socket timeout in milliseconds.
         *
         * @param socketTimeoutMillis the socket timeout
         * @return this builder
         */
        public Builder socketTimeoutMillis(int socketTimeoutMillis) {
            this.socketTimeoutMillis = socketTimeoutMillis;
            return this;
        }

        /**
         * Sets the reconnection delay in milliseconds.
         *
         * @param reconnectDelayMillis the reconnect delay
         * @return this builder
         */
        public Builder reconnectDelayMillis(long reconnectDelayMillis) {
            this.reconnectDelayMillis = reconnectDelayMillis;
            return this;
        }

        /**
         * Sets the Redis client name.
         *
         * @param clientName the client name
         * @return this builder
         */
        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the completed configuration
         */
        public RedisPubSubConfig build() {
            String resolvedClientName = (clientName != null) ? clientName : defaultClientName(serverName);
            return new RedisPubSubConfig(
                host,
                port,
                ssl,
                username,
                password,
                transportChannel,
                serverName,
                proxyServerName,
                connectTimeoutMillis,
                socketTimeoutMillis,
                reconnectDelayMillis,
                resolvedClientName
            );
        }

        private static String defaultClientName(String serverName) {
            String sn = emptyToNull(serverName);
            return (sn != null) ? ("rapunzellib-" + sn) : "rapunzellib";
        }
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null) throw new NullPointerException(name);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return trimmed;
    }

    private static int requirePort(int port) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("port must be between 1 and 65535");
        return port;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be > 0");
        return value;
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must be >= 0");
        return value;
    }

    private static long requireNonNegative(long value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must be >= 0");
        return value;
    }

    private static String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
