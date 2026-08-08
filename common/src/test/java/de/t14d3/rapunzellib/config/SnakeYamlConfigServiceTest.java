package de.t14d3.rapunzellib.config;

import de.t14d3.rapunzellib.context.ResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SnakeYamlConfigServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnakeYamlConfigServiceTest.class);

    private static final String DEFAULT_RESOURCE = "config.yml";
    private static final String DEFAULTS = """
        network:
          enabled: true
          serverName: ""
          queue:
            enabled: true
            flushPeriodSeconds: 2
        modules:
          joinleave: true
        """;

    private static ResourceProvider defaultsProvider() {
        return path -> {
            if (!DEFAULT_RESOURCE.equals(path)) return Optional.empty();
            return Optional.of((InputStream) new ByteArrayInputStream(DEFAULTS.getBytes(StandardCharsets.UTF_8)));
        };
    }

    @Test
    void mergesMissingKeysFromBundledDefaultsOnEveryLoad(@TempDir Path dir) throws Exception {
        // Existing file with only SOME keys: missing keys (including nested
        // ones) must be deep-merged from the bundled default on load, without
        // overwriting the existing values.
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "network:\n  enabled: false\n", StandardCharsets.UTF_8);

        SnakeYamlConfigService service = new SnakeYamlConfigService(defaultsProvider(), LOGGER);
        YamlConfig config = service.load(file, DEFAULT_RESOURCE);

        // Existing user value preserved.
        assertFalse(config.getBoolean("network.enabled", true));
        // Missing keys merged from defaults (nested deep merge).
        assertEquals("", config.getString("network.serverName", "missing"));
        assertTrue(config.getBoolean("network.queue.enabled", false));
        assertEquals(2, config.getInt("network.queue.flushPeriodSeconds", -1));
        assertTrue(config.getBoolean("modules.joinleave", false));
        // Merged values are persisted.
        assertTrue(Files.readString(file).contains("serverName"));
    }

    @Test
    void existingValuesAreNeverOverwrittenByDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "network:\n  serverName: \"lobby\"\n", StandardCharsets.UTF_8);

        SnakeYamlConfigService service = new SnakeYamlConfigService(defaultsProvider(), LOGGER);
        YamlConfig config = service.load(file, DEFAULT_RESOURCE);

        assertEquals("lobby", config.getString("network.serverName", "missing"));
        // Deep merge still fills siblings of the user-configured section.
        assertTrue(config.getBoolean("network.queue.enabled", false));
    }

    @Test
    void fileThatContainsEverythingIsUnchanged(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, DEFAULTS, StandardCharsets.UTF_8);

        SnakeYamlConfigService service = new SnakeYamlConfigService(defaultsProvider(), LOGGER);
        YamlConfig config = service.load(file, DEFAULT_RESOURCE);

        assertEquals("", config.getString("network.serverName", "missing"));
        assertTrue(config.getBoolean("network.queue.enabled", false));
        assertTrue(config.getBoolean("modules.joinleave", false));
    }

    @Test
    void mergeSkipsKeysWithoutBundledDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "custom:\n  key: value\n", StandardCharsets.UTF_8);

        SnakeYamlConfigService service = new SnakeYamlConfigService(defaultsProvider(), LOGGER);
        YamlConfig config = service.load(file, DEFAULT_RESOURCE);

        assertEquals("value", config.getString("custom.key", "missing"));
    }
}
