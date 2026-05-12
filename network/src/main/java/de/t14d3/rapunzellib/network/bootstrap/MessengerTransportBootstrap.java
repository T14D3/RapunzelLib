package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkDefaults;
import de.t14d3.rapunzellib.network.redis.RedisPubSubConfig;
import de.t14d3.rapunzellib.network.redis.RedisPubSubMessenger;
import de.t14d3.rapunzellib.network.rpcserver.RpcServerConfig;
import de.t14d3.rapunzellib.network.rpcserver.RpcServerMessenger;
import de.t14d3.rapunzellib.network.rpcserver.RpcClientConfig;
import de.t14d3.rapunzellib.network.rpcserver.RpcClientMessenger;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Objects;

/**
 * Bootstraps transport-level messengers (Redis, RPC server/client, plugin messaging).
 *
 * <p>Parses configuration YAML and environment variables, applies transport priority
 * logic, and creates the appropriate {@link Messenger} implementation.
 */
@SuppressWarnings("SameParameterValue")
public final class MessengerTransportBootstrap {
    /** Environment variable for overriding the server name. */
    public static final String ENV_SERVER_NAME = "RAPUNZEL_SERVER_NAME";
    /** Environment variable for overriding the proxy server name. */
    public static final String ENV_PROXY_SERVER_NAME = "RAPUNZEL_PROXY_SERVER_NAME";
    /** Environment variable for overriding the RPC host address. */
    public static final String ENV_RPC_HOST = "RAPUNZEL_RPC_HOST";
    /** Environment variable for overriding the RPC port. */
    public static final String ENV_RPC_PORT = "RAPUNZEL_RPC_PORT";

    /**
     * Transport priority selection for messenger initialization.
     */
    public enum TransportPriority {
        REDIS_FIRST,
        PLUGIN_FIRST,
        REDIS_ONLY,
        PLUGIN_ONLY,
        RPC_SERVER_FIRST,
        RPC_SERVER_ONLY;

        /**
         * Parses a transport priority from a configuration value.
         *
         * @param raw the raw config string (case-insensitive)
         * @return the parsed priority, or null if unrecognized
         */
        public static TransportPriority fromConfigValue(String raw) {
            if (raw == null) {
                return null;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "redis_first", "redis-first", "redisfirst" -> REDIS_FIRST;
                case "plugin_first", "plugin-first", "pluginfirst" -> PLUGIN_FIRST;
                case "redis_only", "redis-only", "redisonly", "redis" -> REDIS_ONLY;
                case "plugin_only", "plugin-only", "pluginonly", "plugin" -> PLUGIN_ONLY;
                case "rpc_server_first", "rpc-server-first", "rpcserverfirst", "rpc_first", "rpc-first", "rpcfirst" ->
                        RPC_SERVER_FIRST;
                case "rpc_server_only", "rpc-server-only", "rpcserveronly", "rpc_only", "rpc-only", "rpconly",
                     "rpc_server", "rpc-server", "rpcserver" -> RPC_SERVER_ONLY;
                default -> null;
            };
        }
    }

    /**
     * Result of a transport bootstrap operation.
     *
     * @param messenger  the initialized messenger
     * @param usingRedis whether the messenger uses Redis transport
     * @param closeable  a closeable to clean up resources, or a no-op
     */
    public record Result(Messenger messenger, boolean usingRedis, AutoCloseable closeable) {
    }

    /**
     * Resolved server and proxy names.
     *
     * @param serverName     the resolved server name
     * @param proxyServerName the resolved proxy server name
     */
    public record ResolvedNames(String serverName, String proxyServerName) {
    }

    private MessengerTransportBootstrap() {
    }

    /**
     * Bootstraps a transport messenger using the global Rapunzel context services.
     *
     * @param config     the YAML configuration
     * @param platformId the platform identifier
     * @param logger     the logger
     * @return the transport bootstrap result
     */
    public static Result bootstrap(YamlConfig config, PlatformId platformId, Logger logger) {
        return bootstrap(config, platformId, logger, Rapunzel.context().services());
    }

    /**
     * Bootstraps a transport and registers it into the given {@link ServiceRegistry}.
     *
     * <p>This overload exists so platform bootstraps can configure transports before the global
     * {@code Rapunzel} context is installed.</p>
     */
    public static Result bootstrap(
            YamlConfig config,
            PlatformId platformId,
            Logger logger,
            ServiceRegistry services
    ) {
        return bootstrap(config, platformId, logger, services, resolvePriority(config));
    }

    public static Result bootstrap(
            YamlConfig config,
            PlatformId platformId,
            Logger logger,
            ServiceRegistry services,
            TransportPriority priority
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(services, "services");

        Messenger current = services.get(Messenger.class);
        TransportPriority effective = priority != null ? priority : TransportPriority.PLUGIN_ONLY;

        // Handle PLUGIN_ONLY early return
        if (effective == TransportPriority.PLUGIN_ONLY) {
            return new Result(current, false, NOOP_CLOSEABLE);
        }

        ResolvedNames names = resolveNames(config, platformId);
        String serverName = names.serverName();
        String proxyServerName = names.proxyServerName();

        // Handle RPC_SERVER transports (proxy side)
        if (effective == TransportPriority.RPC_SERVER_FIRST || effective == TransportPriority.RPC_SERVER_ONLY) {
            if (platformId == PlatformId.VELOCITY) {
                Result result = bootstrapRpcServer(config, serverName, proxyServerName, logger, services);
                if (result != null) {
                    return result;
                }
                if (effective == TransportPriority.RPC_SERVER_ONLY) {
                    logger.warn("[Network] RPC_SERVER_ONLY requested but failed to start RPC server; falling back to plugin messaging.");
                    return new Result(current, false, NOOP_CLOSEABLE);
                }

                // RPC_SERVER_FIRST - try other transports
            } else {
                // Backend side - connect as RPC client
                Result result = bootstrapRpcClient(config, serverName, proxyServerName, logger, services);
                if (result != null) {
                    return result;
                }
                if (effective == TransportPriority.RPC_SERVER_ONLY) {
                    logger.warn("[Network] RPC_SERVER_ONLY requested but failed to connect RPC client; falling back to plugin messaging.");
                    return new Result(current, false, NOOP_CLOSEABLE);
                }
            }
        }


        // Handle REDIS transports
        if (effective == TransportPriority.REDIS_FIRST || effective == TransportPriority.REDIS_ONLY) {
            Result result = bootstrapRedis(config, serverName, proxyServerName, logger, services);
            if (result != null) {
                return result;
            }
            if (effective == TransportPriority.REDIS_ONLY) {
                logger.warn("[Network] REDIS_ONLY requested but Redis is not available; falling back to plugin messaging.");
                return new Result(current, false, NOOP_CLOSEABLE);
            }
        }

        // PLUGIN_FIRST or fallback
        return new Result(current, false, NOOP_CLOSEABLE);
    }

    /**
     * Resolves resolved names from configuration, environment variables, and defaults.
     *
     * @param config     the YAML configuration
     * @param platformId the platform identifier
     * @return resolved server and proxy names
     */
    public static ResolvedNames resolveNames(YamlConfig config, PlatformId platformId) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(platformId, "platformId");

        String proxyServerName = firstNonBlank(
                config.getString("network.proxyServerName", null),
                System.getenv(ENV_PROXY_SERVER_NAME),
                NetworkDefaults.DEFAULT_PROXY_SERVER_NAME
        );

        String serverName = firstNonBlank(
                config.getString("network.serverName", null),
                System.getenv(ENV_SERVER_NAME),
                platformId == PlatformId.VELOCITY ? proxyServerName : null
        );

        return new ResolvedNames(serverName, proxyServerName);
    }

    private static Result bootstrapRpcServer(
            YamlConfig config,
            String serverName,
            String proxyServerName,
            Logger logger,
            ServiceRegistry services
    ) {
        if (!config.getBoolean("network.rpcServer.enabled", false)) {
            return null;
        }

        if (serverName == null || serverName.isBlank()) {
            logger.warn("[Network] transport=rpc_server but serverName is not set; cannot start RPC server.");
            return null;
        }

        String host = firstNonBlank(
                config.getString("network.rpcServer.host", null),
                System.getenv(ENV_RPC_HOST),
                NetworkDefaults.DEFAULT_RPC_BIND_HOST
        );
        int port = intInRange(
                config.getLong("network.rpcServer.port", NetworkDefaults.DEFAULT_RPC_PORT),
                1,
                65535,
                NetworkDefaults.DEFAULT_RPC_PORT
        );
        int maxClients = requirePositiveInt(config.getLong("network.rpcServer.maxClients", 100), 100);
        long heartbeatIntervalMillis = requirePositiveLong(config.getLong("network.rpcServer.heartbeatIntervalMillis", 30000), 30000);
        long heartbeatTimeoutMillis = requirePositiveLong(config.getLong("network.rpcServer.heartbeatTimeoutMillis", 60000), 60000);
        long reconnectDelayMillis = requireNonNegativeLong(config.getLong("network.rpcServer.reconnectDelayMillis", 5000), 5000);

        RpcServerConfig.Builder builder = RpcServerConfig.builder(serverName.trim())
                .bindHost(host)
                .port(port)
                .maxClients(maxClients)
                .heartbeatIntervalMillis(heartbeatIntervalMillis)
                .heartbeatTimeoutMillis(heartbeatTimeoutMillis)
                .reconnectDelayMillis(reconnectDelayMillis);

        RpcServerMessenger rpcServer = new RpcServerMessenger(builder.build(), logger);
        services.register(Messenger.class, rpcServer);
        services.register(RpcServerMessenger.class, rpcServer);

        logger.info(
                "[Network] Using RpcServerMessenger (serverName={}, host={}, port={})",
                serverName.trim(),
                host,
                port
        );

        return new Result(rpcServer, false, rpcServer);
    }

    private static Result bootstrapRpcClient(
            YamlConfig config,
            String serverName,
            String proxyServerName,
            Logger logger,
            ServiceRegistry services
    ) {
        if (!config.getBoolean("network.rpcServer.enabled", false)) {
            return null;
        }

        if (serverName == null || serverName.isBlank()) {
            logger.warn("[Network] transport=rpc_client but serverName is not set; cannot connect RPC client.");
            return null;
        }

        String host = firstNonBlank(
                config.getString("network.rpcServer.host", null),
                System.getenv(ENV_RPC_HOST),
                NetworkDefaults.DEFAULT_RPC_HOST
        );
        int port = intInRange(
                config.getLong("network.rpcServer.port", NetworkDefaults.DEFAULT_RPC_PORT),
                1,
                65535,
                NetworkDefaults.DEFAULT_RPC_PORT
        );
        long heartbeatIntervalMillis = requirePositiveLong(config.getLong("network.rpcServer.heartbeatIntervalMillis", 30000), 30000);
        long heartbeatTimeoutMillis = requirePositiveLong(config.getLong("network.rpcServer.heartbeatTimeoutMillis", 60000), 60000);
        long reconnectDelayMillis = requireNonNegativeLong(config.getLong("network.rpcServer.reconnectDelayMillis", 5000), 5000);
        long maxReconnectDelayMillis = requireNonNegativeLong(config.getLong("network.rpcServer.maxReconnectDelayMillis", 60000), 60000);
        double reconnectMultiplier = config.getDouble("network.rpcServer.reconnectMultiplier", 2.0);

        RpcClientConfig.Builder builder = RpcClientConfig.builder(serverName.trim())
                .proxyHost(host)
                .proxyPort(port)
                .heartbeatIntervalMillis(heartbeatIntervalMillis)
                .heartbeatTimeoutMillis(heartbeatTimeoutMillis)
                .reconnectDelayMillis(reconnectDelayMillis)
                .maxReconnectDelayMillis(maxReconnectDelayMillis)
                .reconnectMultiplier(reconnectMultiplier);

        RpcClientMessenger rpcClient = new RpcClientMessenger(builder.build(), logger);
        services.register(Messenger.class, rpcClient);
        services.register(RpcClientMessenger.class, rpcClient);

        logger.info(
                "[Network] Using RpcClientMessenger (serverName={}, proxyServerName={}, host={}, port={})",
                serverName.trim(),
                proxyServerName != null ? proxyServerName.trim() : NetworkDefaults.DEFAULT_PROXY_SERVER_NAME,
                host,
                port
        );

        return new Result(rpcClient, false, rpcClient);
    }

    private static Result bootstrapRedis(
            YamlConfig config,
            String serverName,
            String proxyServerName,
            Logger logger,
            ServiceRegistry services
    ) {
        if (serverName == null || serverName.isBlank()) {
            logger.warn(
                    "[Network] transport=redis but serverName is not set (config: network.serverName or env: {}); falling back to plugin messaging.",
                    ENV_SERVER_NAME
            );
            return null;
        }

        String host = firstNonBlank(config.getString("network.redis.host", null), NetworkDefaults.DEFAULT_RPC_HOST);
        int port = intInRange(
                config.getLong("network.redis.port", NetworkDefaults.DEFAULT_REDIS_PORT),
                1,
                65535,
                NetworkDefaults.DEFAULT_REDIS_PORT
        );
        boolean ssl = config.getBoolean("network.redis.ssl", false);
        String username = blankToNull(config.getString("network.redis.username", null));
        String password = blankToNull(config.getString("network.redis.password", null));
        String transportChannel = firstNonBlank(
                config.getString("network.redis.transportChannel", null),
                "rapunzellib:bridge"
        );
        int connectTimeoutMillis = requirePositiveInt(
                config.getLong("network.redis.connectTimeoutMillis", NetworkDefaults.DEFAULT_REDIS_CONNECT_TIMEOUT_MILLIS),
                NetworkDefaults.DEFAULT_REDIS_CONNECT_TIMEOUT_MILLIS
        );
        int socketTimeoutMillis = requireNonNegativeInt(
                config.getLong("network.redis.socketTimeoutMillis", NetworkDefaults.DEFAULT_REDIS_SOCKET_TIMEOUT_MILLIS),
                NetworkDefaults.DEFAULT_REDIS_SOCKET_TIMEOUT_MILLIS
        );
        long reconnectDelayMillis = requireNonNegativeLong(
                config.getLong("network.redis.reconnectDelayMillis", NetworkDefaults.DEFAULT_REDIS_RECONNECT_DELAY_MILLIS),
                NetworkDefaults.DEFAULT_REDIS_RECONNECT_DELAY_MILLIS
        );
        String clientName = blankToNull(config.getString("network.redis.clientName", null));

        RedisPubSubConfig.Builder builder = RedisPubSubConfig.builder()
                .host(host)
                .port(port)
                .ssl(ssl)
                .transportChannel(transportChannel)
                .serverName(serverName.trim())
                .proxyServerName(proxyServerName.trim())
                .connectTimeoutMillis(connectTimeoutMillis)
                .socketTimeoutMillis(socketTimeoutMillis)
                .reconnectDelayMillis(reconnectDelayMillis);

        if (username != null) builder.username(username);
        if (password != null) builder.password(password);
        if (clientName != null) builder.clientName(clientName);

        RedisPubSubMessenger redis = new RedisPubSubMessenger(builder.build(), logger);
        services.register(Messenger.class, redis);
        services.register(RedisPubSubMessenger.class, redis);

        logger.info(
                "[Network] Using RedisPubSubMessenger (serverName={}, proxyServerName={}, host={}, port={}, channel={})",
                serverName.trim(),
                proxyServerName.trim(),
                host,
                port,
                transportChannel
        );

        return new Result(redis, true, redis);
    }

    private static final AutoCloseable NOOP_CLOSEABLE = () -> {
    };

    private static String normalize(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves the transport priority from configuration.
     *
     * @param config the YAML configuration
     * @return the resolved transport priority
     */
    public static TransportPriority resolvePriority(YamlConfig config) {
        if (config == null) {
            return TransportPriority.PLUGIN_ONLY;
        }
        TransportPriority priority = TransportPriority.fromConfigValue(
                config.getString("network.transportPriority", null)
        );
        if (priority != null) {
            return priority;
        }
        String transport = normalize(config.getString("network.transport", "plugin"));
        TransportPriority fromTransport = TransportPriority.fromConfigValue(transport);
        return fromTransport != null ? fromTransport : TransportPriority.PLUGIN_ONLY;
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) return null;
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String trimmed = candidate.trim();
            if (!trimmed.isBlank()) return trimmed;
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static int intInRange(long value, int min, int max, int fallback) {
        if (value < min || value > max) return fallback;
        return (int) value;
    }

    private static int requirePositiveInt(long value, int fallback) {
        if (value <= 0) return fallback;
        if (value > Integer.MAX_VALUE) return fallback;
        return (int) value;
    }

    private static int requireNonNegativeInt(long value, int fallback) {
        if (value < 0) return fallback;
        if (value > Integer.MAX_VALUE) return fallback;
        return (int) value;
    }

    private static long requireNonNegativeLong(long value, long fallback) {
        if (value < 0) return fallback;
        return value;
    }

    private static long requirePositiveLong(long value, long fallback) {
        if (value <= 0) return fallback;
        return value;
    }
}
