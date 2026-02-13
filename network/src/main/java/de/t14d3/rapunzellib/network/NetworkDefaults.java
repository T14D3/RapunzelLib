package de.t14d3.rapunzellib.network;

import org.jetbrains.annotations.NotNull;

public final class NetworkDefaults {
    public static final String DEFAULT_PROXY_SERVER_NAME = "velocity";
    public static final String DEFAULT_RPC_BIND_HOST = "0.0.0.0";
    public static final String DEFAULT_RPC_HOST = "127.0.0.1";
    public static final int DEFAULT_RPC_PORT = 25566;
    public static final int DEFAULT_REDIS_PORT = 6379;
    public static final int DEFAULT_REDIS_CONNECT_TIMEOUT_MILLIS = 5_000;
    public static final int DEFAULT_REDIS_SOCKET_TIMEOUT_MILLIS = 5_000;
    public static final long DEFAULT_REDIS_RECONNECT_DELAY_MILLIS = 2_000;

    private NetworkDefaults() {
    }

    public static @NotNull String defaultProxyServerName() {
        return DEFAULT_PROXY_SERVER_NAME;
    }
}
