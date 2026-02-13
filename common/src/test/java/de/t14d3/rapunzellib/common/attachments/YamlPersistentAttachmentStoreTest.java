package de.t14d3.rapunzellib.common.attachments;

import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.SnakeYamlConfigService;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.nbt.RNbtByteArray;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class YamlPersistentAttachmentStoreTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlPersistentAttachmentStoreTest.class);
    private static final ResourceProvider EMPTY_RESOURCES = path -> Optional.empty();

    @TempDir
    Path tempDir;

    @Test
    void roundTripsFlatAttachmentRootsAcrossReloads() {
        Path file = tempDir.resolve("attachments.yml");
        ConfigService configService = new SnakeYamlConfigService(EMPTY_RESOURCES, LOGGER);
        RNbtCompound expected = RNbtCompound.builder()
            .putString("name", "Rapunzel")
            .putInt("level", 7)
            .putBoolean("enabled", true)
            .putByteArray("blob", new byte[] {1, 2, 3})
            .build();

        try (YamlPersistentAttachmentStore store = new YamlPersistentAttachmentStore(LOGGER, configService, file)) {
            store.put("players.test-user", expected);
            assertEquals(expected, store.get("players.test-user"));
        }

        try (YamlPersistentAttachmentStore reloaded = new YamlPersistentAttachmentStore(LOGGER, configService, file)) {
            RNbtCompound actual = reloaded.get("players.test-user");
            assertEquals("Rapunzel", actual.get("name").orElseThrow().asPrimitive().stringValue());
            assertEquals(7, actual.get("level").orElseThrow().asPrimitive().intValue());
            assertTrue(actual.get("enabled").orElseThrow().asPrimitive().booleanValue());
            assertArrayEquals(new byte[] {1, 2, 3}, ((RNbtByteArray) actual.get("blob").orElseThrow()).value());
        }
    }
}
