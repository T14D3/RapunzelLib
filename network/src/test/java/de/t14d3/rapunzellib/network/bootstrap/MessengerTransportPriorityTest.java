package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.config.YamlConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class MessengerTransportPriorityTest {
    @Test
    void defaultsToPluginOnlyWhenNoValuesPresent() {
        YamlConfig config = new MapConfig(Map.of());
        assertEquals(
            MessengerTransportBootstrap.TransportPriority.PLUGIN_ONLY,
            MessengerTransportBootstrap.resolvePriority(config)
        );
    }

    @Test
    void transportPriorityOverridesTransport() {
        YamlConfig config = new MapConfig(Map.of(
            "network.transport", "redis",
            "network.transportPriority", "rpc_server_first"
        ));

        assertEquals(
            MessengerTransportBootstrap.TransportPriority.RPC_SERVER_FIRST,
            MessengerTransportBootstrap.resolvePriority(config)
        );
    }


    @Test
    void fallsBackToPluginOnlyForUnknownValues() {
        YamlConfig config = new MapConfig(Map.of("network.transport", "unknown_mode"));
        assertEquals(
            MessengerTransportBootstrap.TransportPriority.PLUGIN_ONLY,
            MessengerTransportBootstrap.resolvePriority(config)
        );
    }

    @Test
    void resolveNamesUsesSharedDefaultsForProxyRuntime() {
        MessengerTransportBootstrap.ResolvedNames names = MessengerTransportBootstrap.resolveNames(
            new MapConfig(Map.of()),
            PlatformId.VELOCITY
        );

        assertEquals("velocity", names.proxyServerName());
        assertEquals("velocity", names.serverName());
    }

    @Test
    void resolveNamesLeavesBackendServerUnsetWithoutConfig() {
        MessengerTransportBootstrap.ResolvedNames names = MessengerTransportBootstrap.resolveNames(
            new MapConfig(Map.of()),
            PlatformId.PAPER
        );

        assertEquals("velocity", names.proxyServerName());
        assertNull(names.serverName());
    }

    @Test
    void resolveNamesHonorsConfiguredValues() {
        MessengerTransportBootstrap.ResolvedNames names = MessengerTransportBootstrap.resolveNames(
            new MapConfig(Map.of(
                "network.proxyServerName", "edge-proxy",
                "network.serverName", "survival-1"
            )),
            PlatformId.PAPER
        );

        assertEquals("edge-proxy", names.proxyServerName());
        assertEquals("survival-1", names.serverName());
    }

    private static final class MapConfig implements YamlConfig {
        private final Map<String, Object> values;

        private MapConfig(Map<String, Object> values) {
            this.values = new HashMap<>(values);
        }

        @Override
        public boolean contains(@NotNull String path) {
            return values.containsKey(path);
        }

        @Override
        public @NotNull Set<String> keys(boolean deep) {
            return Set.copyOf(values.keySet());
        }

        @Override
        public @Nullable Object get(@NotNull String path) {
            return values.get(path);
        }

        @Override
        public @Nullable String getString(@NotNull String path, @Nullable String def) {
            Object value = values.get(path);
            return value instanceof String s ? s : def;
        }

        @Override
        public int getInt(@NotNull String path, int def) {
            Object value = values.get(path);
            if (value instanceof Number number) {
                return number.intValue();
            }
            return def;
        }

        @Override
        public boolean getBoolean(@NotNull String path, boolean def) {
            Object value = values.get(path);
            return value instanceof Boolean b ? b : def;
        }

        @Override
        public double getDouble(@NotNull String path, double def) {
            Object value = values.get(path);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            return def;
        }

        @Override
        public void set(@NotNull String path, @Nullable Object value) {
            if (value == null) {
                values.remove(path);
            } else {
                values.put(path, value);
            }
        }

        @Override
        public @Nullable String getComment(@NotNull String path) {
            return null;
        }

        @Override
        public void setComment(@NotNull String path, @NotNull String comment) {
        }

        @Override
        public void save() {
        }

        @Override
        public void reload() {
        }
    }
}
