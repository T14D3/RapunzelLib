package de.t14d3.rapunzellib.devrunner;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the devrunner bot-address resolver (velocity-proxy policy).
 */
final class DevRunnerBotAddressResolverTest {

    private static DevRunnerConfig.ServerSpec server(String platform, int port) {
        return new DevRunnerConfig.ServerSpec(platform, "latest", port, null, java.util.List.of(), Map.of());
    }

    private static DevRunnerConfig.ServerSpec velocity(int port) {
        return server("velocity", port);
    }

    private static Map<String, DevRunnerConfig.ServerSpec> topology(
            DevRunnerConfig.ServerSpec velocity, DevRunnerConfig.ServerSpec... backends
    ) {
        Map<String, DevRunnerConfig.ServerSpec> servers = new LinkedHashMap<>();
        if (velocity != null) servers.put("velocity", velocity);
        for (int i = 0; i < backends.length; i++) {
            servers.put("backend-" + (i + 1), backends[i]);
        }
        return servers;
    }

    @Test
    void routesThroughProxyWhenVelocityConfigured() {
        var servers = topology(velocity(25577), server("paper", 26565), server("paper", 26566));

        assertEquals("backend-1.example.com:25577",
                DevRunnerOrchestrator.resolveBotAddress(servers, "backend-1", velocity(25577), false));
        assertEquals("backend-2.example.com:25577",
                DevRunnerOrchestrator.resolveBotAddress(servers, "backend-2", velocity(25577), true));
    }

    @Test
    void neverTargetsTheProxyAsBackend() {
        var servers = topology(velocity(25577), server("paper", 26565));

        assertNull(DevRunnerOrchestrator.resolveBotAddress(servers, "velocity", velocity(25577), false));
        assertNull(DevRunnerOrchestrator.resolveBotAddress(servers, "velocity", velocity(25577), true));
    }

    @Test
    void unknownServerReturnsNull() {
        var servers = topology(velocity(25577), server("paper", 26565));

        assertNull(DevRunnerOrchestrator.resolveBotAddress(servers, "nope", velocity(25577), false));
    }

    @Test
    void fallsBackToDirectAddressWhenExplicitlyAllowed() {
        var servers = topology(null, server("paper", 26565));

        assertEquals("127.0.0.1:26565",
                DevRunnerOrchestrator.resolveBotAddress(servers, "backend-1", null, true));
    }

    @Test
    void failsWhenNoVelocityAndDirectNotAllowed() {
        var servers = topology(null, server("paper", 26565));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> DevRunnerOrchestrator.resolveBotAddress(servers, "backend-1", null, false));
        assertTrue(ex.getMessage().contains("allowDirectConnections"), "message should mention the opt-out flag");
    }

    @Test
    void emptyTopologyReturnsNull() {
        assertNull(DevRunnerOrchestrator.resolveBotAddress(Map.of(), "backend-1", null, true));
    }

    @Test
    void configRoundTripsAllowDirectConnectionsFlag() throws Exception {
        Path json = java.nio.file.Files.createTempFile("devrunner-allow-direct", ".json");
        try {
            DevRunnerConfig direct = new DevRunnerConfig(
                    "java", java.util.List.of(), Path.of("run/devrunner"), Path.of("run/devrunner/cache"),
                    Path.of("run/devrunner/instances"), Map.of(), Map.of(),
                    new DevRunnerConfig.LiveTestConfig(false, false, null, java.util.List.of(), 30_000L, 300_000L),
                    java.util.List.of(), Map.of(), false, "profile", true
            );
            DevRunnerConfigParser.writeJson(direct, json);
            DevRunnerConfig parsed = DevRunnerConfigParser.parseJsonFile(json);
            assertTrue(parsed.allowDirectConnections(), "allowDirectConnections should survive JSON round-trip");

            DevRunnerConfig proxyFirst = new DevRunnerConfig(
                    "java", java.util.List.of(), Path.of("run/devrunner"), Path.of("run/devrunner/cache"),
                    Path.of("run/devrunner/instances"), Map.of(), Map.of(),
                    new DevRunnerConfig.LiveTestConfig(false, false, null, java.util.List.of(), 30_000L, 300_000L),
                    java.util.List.of(), Map.of(), false, "profile", false
            );
            DevRunnerConfigParser.writeJson(proxyFirst, json);
            parsed = DevRunnerConfigParser.parseJsonFile(json);
            assertEquals(false, parsed.allowDirectConnections(), "absent flag must default to false (proxy required)");
        } finally {
            java.nio.file.Files.deleteIfExists(json);
        }
    }
}
