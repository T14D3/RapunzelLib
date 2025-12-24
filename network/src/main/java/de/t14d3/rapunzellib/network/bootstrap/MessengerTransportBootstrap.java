package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.redis.RedisPubSubConfig;
import de.t14d3.rapunzellib.network.redis.RedisPubSubMessenger;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Objects;

public final class MessengerTransportBootstrap {
    public static final String ENV_SERVER_NAME = "RAPUNZEL_SERVER_NAME";
    public static final String ENV_PROXY_SERVER_NAME = "RAPUNZEL_PROXY_SERVER_NAME";

    public record Result(Messenger messenger, boolean usingRedis, AutoCloseable closeable) {
    }

    private MessengerTransportBootstrap() {
    }

    public static Result bootstrap(YamlConfig config, PlatformId platformId, Logger logger) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(logger, "logger");

        Messenger current = Rapunzel.context().services().get(Messenger.class);

        String transport = normalize(config.getString("network.transport", "plugin"));
        if (!"redis".equals(transport)) {
            return new Result(current, false, NOOP_CLOSEABLE);
        }

        String proxyServerName = firstNonBlank(
            config.getString("network.proxyServerName", null),
            System.getenv(ENV_PROXY_SERVER_NAME),
            "velocity"
        );

        String serverName = firstNonBlank(
            config.getString("network.serverName", null),
            System.getenv(ENV_SERVER_NAME),
            platformId == PlatformId.VELOCITY ? proxyServerName : null
        );

        if (serverName == null || serverName.isBlank()) {
            logger.warn(
                "[Network] transport=redis but serverName is not set (config: network.serverName or env: {}); falling back to plugin messaging.",
                ENV_SERVER_NAME
            );
            return new Result(current, false, NOOP_CLOSEABLE);
        }

        String host = firstNonBlank(config.getString("network.redis.host", null), "127.0.0.1");
        int port = intInRange(config.getLong("network.redis.port", 6379), 1, 65535, 6379);
        boolean ssl = config.getBoolean("network.redis.ssl", false);
        String username = blankToNull(config.getString("network.redis.username", null));
        String password = blankToNull(config.getString("network.redis.password", null));
        String transportChannel = firstNonBlank(
            config.getString("network.redis.transportChannel", null),
            "rapunzellib:bridge"
        );
        int connectTimeoutMillis = requirePositiveInt(config.getLong("network.redis.connectTimeoutMillis", 2_000), 2_000);
        int socketTimeoutMillis = requireNonNegativeInt(config.getLong("network.redis.socketTimeoutMillis", 5_000), 5_000);
        long reconnectDelayMillis = requireNonNegativeLong(config.getLong("network.redis.reconnectDelayMillis", 2_000), 2_000);
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
        Rapunzel.context().services().register(Messenger.class, redis);
        Rapunzel.context().services().register(RedisPubSubMessenger.class, redis);

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
}

