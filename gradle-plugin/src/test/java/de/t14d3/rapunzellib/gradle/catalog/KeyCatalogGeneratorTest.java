package de.t14d3.rapunzellib.gradle.catalog;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyCatalogGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void parseInputFilesReadsNewlineDelimitedKeyEntries() throws Exception {
        Path input = tempDir.resolve("blocks.txt");
        Files.writeString(
            input,
            """
            # comment
            minecraft:stone

            // another comment
            minecraft:oak_log
            rapunzellib:bridge
            """
        );

        List<NamespacedKeyEntry> keys = KeyCatalogGenerator.parseInputFiles(List.of(input.toFile()));

        assertEquals(
            List.of(
                new NamespacedKeyEntry("minecraft", "oak_log"),
                new NamespacedKeyEntry("minecraft", "stone"),
                new NamespacedKeyEntry("rapunzellib", "bridge")
            ),
            keys
        );
    }

    @Test
    void renderJavaSourceGeneratesNestedConstantClassesAndCollisionSafeIdentifiers() {
        String source = KeyCatalogGenerator.renderJavaSource(
            "com.example.keys",
            "BlockKeys",
            "blocks",
            List.of(
                new NamespacedKeyEntry("minecraft", "oak-log"),
                new NamespacedKeyEntry("minecraft", "oak_log"),
                new NamespacedKeyEntry("my-mod", "123/path"),
                new NamespacedKeyEntry("my_mod", "123.path")
            )
        );

        assertTrue(source.contains("import de.t14d3.rapunzellib.objects.RKey;"));
        assertTrue(source.contains("public final class BlockKeys"));
        assertTrue(source.contains("public static final String DOMAIN = \"blocks\";"));
        assertTrue(source.contains("public static final class Minecraft"));
        assertTrue(source.contains("public static final RKey OAK_LOG = RKey.of(\"minecraft:oak-log\");"));
        assertTrue(source.contains("public static final RKey OAK_LOG_2 = RKey.of(\"minecraft:oak_log\");"));
        assertTrue(source.contains("public static final class MyMod"));
        assertTrue(source.contains("public static final class MyMod2"));
        assertTrue(source.contains("public static final RKey KEY_123_PATH = RKey.of(\"my-mod:123/path\");"));
        assertTrue(source.contains("public static final String NAMESPACE = \"my_mod\";"));
        assertTrue(source.contains("public static final RKey KEY_123_PATH = RKey.of(\"my_mod:123.path\");"));
        assertFalse(source.contains("public static final class EntityTypes"));
        assertFalse(source.contains("enum "));
    }

    @Test
    void renderJavaSourceAddsConfiguredRuntimeRegistryHelpersOnlyWhenRequested() {
        String source = KeyCatalogGenerator.renderJavaSource(
            "com.example.keys",
            "TypeKeys",
            "types",
            List.of(new NamespacedKeyEntry("minecraft", "zombie")),
            Set.of("entity-types", "item-types", "block-types")
        );

        assertTrue(source.contains("import java.util.Optional;"));
        assertTrue(source.contains("import de.t14d3.rapunzellib.Rapunzel;"));
        assertTrue(source.contains("import de.t14d3.rapunzellib.registry.RBlockType;"));
        assertTrue(source.contains("import de.t14d3.rapunzellib.registry.REntityType;"));
        assertTrue(source.contains("import de.t14d3.rapunzellib.registry.RItemType;"));
        assertTrue(source.contains("public static final class BlockTypes"));
        assertTrue(source.contains("return Rapunzel.blockTypes().find(key);"));
        assertTrue(source.contains("return Rapunzel.blockTypes().require(key);"));
        assertTrue(source.contains("public static final class EntityTypes"));
        assertTrue(source.contains("return REntityType.find(key);"));
        assertTrue(source.contains("return REntityType.require(key);"));
        assertTrue(source.contains("public static final class ItemTypes"));
        assertTrue(source.contains("return RItemType.find(key);"));
        assertTrue(source.contains("return RItemType.require(key);"));
    }

    @Test
    void parseInputFilesRejectsInvalidEntriesWithSourceLocation() throws Exception {
        Path input = tempDir.resolve("invalid.txt");
        Files.writeString(input, "not-a-key\n");

        GradleException failure = assertThrows(GradleException.class, () -> KeyCatalogGenerator.parseInputFiles(List.of(input.toFile())));

        assertTrue(failure.getMessage().contains("invalid.txt:1"));
        assertTrue(failure.getMessage().contains("Expected '<namespace>:<path>'"));
    }

    @Test
    void renderJavaSourceRejectsUnsupportedRuntimeHelperKinds() {
        GradleException failure = assertThrows(
            GradleException.class,
            () -> KeyCatalogGenerator.renderJavaSource(
                "com.example.keys",
                "TypeKeys",
                "types",
                List.of(new NamespacedKeyEntry("minecraft", "zombie")),
                Set.of("blocks")
            )
        );

        assertTrue(failure.getMessage().contains("Unsupported key catalog registry helper 'blocks'"));
        assertTrue(failure.getMessage().contains("block-types"));
        assertTrue(failure.getMessage().contains("entity-types"));
        assertTrue(failure.getMessage().contains("item-types"));
    }
}
