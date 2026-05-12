package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.NotNull;

/**
 * Central constants providing default values for network configuration.
 *
 * <p>All defaults can be overridden through YAML config or environment variables.
 * This class exists to avoid magic values scattered across the codebase.
 */
public final class NetworkDefaults {
    /** Default name for the proxy server. */
    public static final String DEFAULT_PROXY_SERVER_NAME = "velocity";
    /** Default bind host for the RPC server socket. */
    public static final String DEFAULT_RPC_BIND_HOST = "0.0.0.0";
    /** Default host for RPC client connections. */
    public static final String DEFAULT_RPC_HOST = "127.0.0.1";
    /** Default TCP port for RPC communication. */
    public static final int DEFAULT_RPC_PORT = 25566;
    /** Default Redis server port. */
    public static final int DEFAULT_REDIS_PORT = 6379;
    /** Default Redis connection timeout in milliseconds. */
    public static final int DEFAULT_REDIS_CONNECT_TIMEOUT_MILLIS = 5_000;
    /** Default Redis socket timeout in milliseconds. */
    public static final int DEFAULT_REDIS_SOCKET_TIMEOUT_MILLIS = 5_000;
    /** Default delay before Redis reconnection attempts in milliseconds. */
    public static final long DEFAULT_REDIS_RECONNECT_DELAY_MILLIS = 2_000;

    private NetworkDefaults() {
    }

    /**
     * Returns the default proxy server name.
     *
     * @return the default proxy server name
     */
    public static @NotNull String defaultProxyServerName() {
        return DEFAULT_PROXY_SERVER_NAME;
    }
}
