package de.t14d3.rapunzellib.config;

import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RWorldRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class SnakeYamlConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnakeYamlConfigTest.class);

    @Test
    void getSetAndTypeCoercion(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, """
            foo: 123
            bar: "true"
            uuid: "550e8400-e29b-41d4-a716-446655440000"
            duration: 5m
            pos:
              x: 1
              y: 2
              z: 3
            loc:
              world:
                key: "minecraft:overworld"
              x: 1.5
              y: 64
              z: -10
            """, StandardCharsets.UTF_8);

        ResourceProvider emptyResources = _path -> Optional.empty();
        YamlConfig config = new SnakeYamlConfigService(emptyResources, LOGGER).load(file);

        assertEquals(123, config.getInt("foo", 0));
        assertTrue(config.getBoolean("bar", false));

        UUID uuid = config.getUUID("uuid", null);
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), uuid);

        assertEquals(Duration.ofMinutes(5), config.getDuration("duration", null));

        assertEquals(new RBlockPos(1, 2, 3), config.getBlockPos("pos", null));
        assertEquals("minecraft:overworld", config.getString("loc.world.key"));

        RLocation loc = config.getLocation("loc", null);
        assertNotNull(loc);
        assertEquals(new RWorldRef(null, "minecraft:overworld"), loc.world());
        assertEquals(1.5D, loc.x());
        assertEquals(64D, loc.y());
        assertEquals(-10D, loc.z());
    }

    @Test
    void parsesAndWritesComments(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, """
            # Foo comment
            foo: bar
            nested:
              # Baz comment
              baz: 1
            """, StandardCharsets.UTF_8);

        ResourceProvider emptyResources = _path -> Optional.empty();
        YamlConfig config = new SnakeYamlConfigService(emptyResources, LOGGER).load(file);

        assertEquals("Foo comment", config.getComment("foo"));
        assertEquals("Baz comment", config.getComment("nested.baz"));

        config.setComment("nested.baz", "New comment");
        config.save();

        String saved = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(saved.contains("# New comment"));
        assertTrue(saved.contains("baz: 1"));
    }

    @Test
    void mergesDefaultsKeysAndComments(@TempDir Path dir) throws Exception {    
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "present: 1\n", StandardCharsets.UTF_8);        

        String defaults = """
            # Present comment
            present: 2
            # Missing comment
            missing: 3
            """;

        ResourceProvider resources = path -> {
            if (!path.equals("defaults.yml")) return Optional.empty();
            return Optional.of(new ByteArrayInputStream(defaults.getBytes(StandardCharsets.UTF_8)));
        };

        YamlConfig config = new SnakeYamlConfigService(resources, LOGGER).load(file, "defaults.yml");

        assertEquals(1, config.getInt("present", 0));
        assertEquals(3, config.getInt("missing", 0));
        assertEquals("Present comment", config.getComment("present"));
        assertEquals("Missing comment", config.getComment("missing"));
    }

    @Test
    void configurationSectionsSupportKeysAndNestedAccess(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, """
            root: 1
            nested:
              a: "x"
              child:
                b: 2
            """, StandardCharsets.UTF_8);

        ResourceProvider emptyResources = _path -> Optional.empty();
        YamlConfig config = new SnakeYamlConfigService(emptyResources, LOGGER).load(file);

        ConfigurationSection nested = config.getConfigurationSection("nested");
        assertNotNull(nested);
        assertTrue(nested.getKeys(false).contains("a"));
        assertTrue(nested.getKeys(false).contains("child"));
        assertEquals("x", nested.getString("a"));

        assertTrue(nested.getKeys(true).contains("child.b"));
        ConfigurationSection child = nested.getConfigurationSection("child");
        assertNotNull(child);
        assertEquals(2, child.getInt("b", 0));

        ConfigurationSection created = config.createSection("created.section");
        created.set("value", "ok");
        config.save();

        YamlConfig reloaded = new SnakeYamlConfigService(emptyResources, LOGGER).load(file);
        assertEquals("ok", reloaded.getString("created.section.value"));
    }
}

