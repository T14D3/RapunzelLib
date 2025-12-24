package de.t14d3.rapunzellib.network.redis;

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

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public boolean ssl() {
        return ssl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String transportChannel() {
        return transportChannel;
    }

    public String serverName() {
        return serverName;
    }

    public String proxyServerName() {
        return proxyServerName;
    }

    public int connectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int socketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public long reconnectDelayMillis() {
        return reconnectDelayMillis;
    }

    public String clientName() {
        return clientName;
    }

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

    public static final class Builder {
        private String host = "127.0.0.1";
        private int port = 6379;
        private boolean ssl;
        private String username;
        private String password;
        private String transportChannel = "rapunzellib:bridge";
        private String serverName;
        private String proxyServerName = "velocity";
        private int connectTimeoutMillis = 5_000;
        private int socketTimeoutMillis = 5_000;
        private long reconnectDelayMillis = 2_000;
        private String clientName;

        private Builder() {
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder transportChannel(String transportChannel) {
            this.transportChannel = transportChannel;
            return this;
        }

        public Builder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public Builder proxyServerName(String proxyServerName) {
            this.proxyServerName = proxyServerName;
            return this;
        }

        public Builder connectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        public Builder socketTimeoutMillis(int socketTimeoutMillis) {
            this.socketTimeoutMillis = socketTimeoutMillis;
            return this;
        }

        public Builder reconnectDelayMillis(long reconnectDelayMillis) {
            this.reconnectDelayMillis = reconnectDelayMillis;
            return this;
        }

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

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

