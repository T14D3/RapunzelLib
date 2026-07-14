package de.t14d3.rapunzellib.gradle.catalog;

import de.t14d3.rapunzellib.gradle.testutil.TestSupport;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateKeyCatalogTaskFunctionalTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultGenerationModeEmitsRKeyConstantsAndCompilesConsumerUsage() throws Exception {
        writeConsumerProject(
            """
            plugins {
                id 'java'
                id 'de.t14d3.rapunzellib'
            }

            rapunzellib {
                contextWrapper {
                    enabled.set(false)
                }
                keyCatalog {
                    packageName.set('com.example.catalog')
                    className.set('GeneratedKeys')
                    domainName.set('demo')
                }
            }
            """
        );
        TestSupport.writeFile(tempDir, "src/main/rapunzellib/keys.txt", "minecraft:stone\nminecraft:oak_log");
        writeRKeyType();
        TestSupport.writeFile(
            tempDir,
            "src/main/java/com/example/UsesGeneratedKeys.java",
            """
            package com.example;

            import com.example.catalog.GeneratedKeys;
            import de.t14d3.rapunzellib.objects.RKey;

            public final class UsesGeneratedKeys {
                private UsesGeneratedKeys() {
                }

                public static RKey stone() {
                    return GeneratedKeys.Minecraft.STONE;
                }
            }
            """
        );

        BuildResult result = TestSupport.runGradle(tempDir, "rapunzellibGenerateKeyCatalog", "compileJava");

        assertEquals(SUCCESS, result.task(":rapunzellibGenerateKeyCatalog").getOutcome());
        assertEquals(SUCCESS, result.task(":compileJava").getOutcome());

        Path generatedSource = generatedSource("com/example/catalog/GeneratedKeys.java");
        assertTrue(Files.exists(generatedSource));
        String generated = Files.readString(generatedSource);
        assertTrue(generated.contains("public static final RKey STONE = RKey.of(\"minecraft:stone\");"));
        assertFalse(generated.contains("public static final class EntityTypes"));
        assertTrue(Files.exists(compiledClass("com/example/UsesGeneratedKeys.class")));
    }

    @Test
    void helperEnabledGenerationModeEmitsRegistryHelpersAndCompilesConsumerUsage() throws Exception {
        writeConsumerProject(
            """
            plugins {
                id 'java'
                id 'de.t14d3.rapunzellib'
            }

            rapunzellib {
                contextWrapper {
                    enabled.set(false)
                }
                keyCatalog {
                    packageName.set('com.example.catalog')
                    className.set('GeneratedKeys')
                    domainName.set('demo')
                    registryHelpers.set(['entity-types', 'item-types', 'block-types'])
                }
            }
            """
        );
        TestSupport.writeFile(tempDir, "src/main/rapunzellib/keys.txt", "minecraft:zombie\nminecraft:diamond_sword\nminecraft:stone");
        writeRKeyType();
        writeRegistryFixtureTypes();
        TestSupport.writeFile(
            tempDir,
            "src/main/java/com/example/UsesGeneratedHelpers.java",
            """
            package com.example;

            import com.example.catalog.GeneratedKeys;

            public final class UsesGeneratedHelpers {
                private UsesGeneratedHelpers() {
                }

                public static Object resolve() {
                    return GeneratedKeys.EntityTypes.require(GeneratedKeys.Minecraft.ZOMBIE);
                }
            }
            """
        );

        BuildResult result = TestSupport.runGradle(tempDir, "rapunzellibGenerateKeyCatalog", "compileJava");

        assertEquals(SUCCESS, result.task(":rapunzellibGenerateKeyCatalog").getOutcome());
        assertEquals(SUCCESS, result.task(":compileJava").getOutcome());

        String generated = Files.readString(generatedSource("com/example/catalog/GeneratedKeys.java"));
        assertTrue(generated.contains("public static final class BlockTypes"));
        assertTrue(generated.contains("return Rapunzel.blockTypes().find(key);"));
        assertTrue(generated.contains("public static final class EntityTypes"));
        assertTrue(generated.contains("public static final class ItemTypes"));
        assertTrue(Files.exists(compiledClass("com/example/UsesGeneratedHelpers.class")));
    }

    private void writeConsumerProject(String buildFile) {
        TestSupport.writeFile(tempDir, "settings.gradle", "rootProject.name = 'consumer-key-catalog'");
        TestSupport.writeFile(tempDir, "build.gradle", buildFile.strip());
    }

    private void writeRKeyType() {
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/objects/RKey.java",
            """
            package de.t14d3.rapunzellib.objects;

            public final class RKey {
                private final String value;

                private RKey(String value) {
                    this.value = value;
                }

                public static RKey of(String value) {
                    return new RKey(value);
                }

                public String value() {
                    return value;
                }
            }
            """
        );
    }

    private void writeRegistryFixtureTypes() {
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/Rapunzel.java",
            """
            package de.t14d3.rapunzellib;

            import de.t14d3.rapunzellib.objects.RKey;
            import de.t14d3.rapunzellib.registry.RBlockType;
            import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
            import java.util.Optional;

            public final class Rapunzel {
                private static final RBlockTypeRegistry BLOCK_TYPES = new InMemoryBlockTypeRegistry();

                private Rapunzel() {
                }

                public static RBlockTypeRegistry blockTypes() {
                    return BLOCK_TYPES;
                }

                private static final class InMemoryBlockTypeRegistry implements RBlockTypeRegistry {
                    @Override
                    public Optional<RBlockType> find(RKey key) {
                        return Optional.of(new SimpleBlockType(key));
                    }

                    @Override
                    public RBlockType require(RKey key) {
                        return new SimpleBlockType(key);
                    }
                }

                private static final class SimpleBlockType implements RBlockType {
                    private final RKey key;

                    private SimpleBlockType(RKey key) {
                        this.key = key;
                    }

                    @Override
                    public RKey key() {
                        return key;
                    }
                }
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/registry/REntityType.java",
            """
            package de.t14d3.rapunzellib.registry;

            import de.t14d3.rapunzellib.objects.RKey;
            import java.util.Optional;

            public final class REntityType {
                private final RKey key;

                private REntityType(RKey key) {
                    this.key = key;
                }

                public static Optional<REntityType> find(RKey key) {
                    return Optional.of(new REntityType(key));
                }

                public static REntityType require(RKey key) {
                    return new REntityType(key);
                }

                public RKey key() {
                    return key;
                }
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/registry/RBlockType.java",
            """
            package de.t14d3.rapunzellib.registry;

            import de.t14d3.rapunzellib.objects.RKey;

            public interface RBlockType {
                RKey key();
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/registry/RBlockTypeRegistry.java",
            """
            package de.t14d3.rapunzellib.registry;

            import de.t14d3.rapunzellib.objects.RKey;
            import java.util.Optional;

            public interface RBlockTypeRegistry {
                Optional<RBlockType> find(RKey key);

                RBlockType require(RKey key);
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/registry/RItemType.java",
            """
            package de.t14d3.rapunzellib.registry;

            import de.t14d3.rapunzellib.objects.RKey;
            import java.util.Optional;

            public final class RItemType {
                private final RKey key;

                private RItemType(RKey key) {
                    this.key = key;
                }

                public static Optional<RItemType> find(RKey key) {
                    return Optional.of(new RItemType(key));
                }

                public static RItemType require(RKey key) {
                    return new RItemType(key);
                }

                public RKey key() {
                    return key;
                }
            }
            """
        );
    }

    private Path generatedSource(String relativePath) {
        return tempDir.resolve("src/generated/java/" + relativePath);
    }

    private Path compiledClass(String relativePath) {
        return tempDir.resolve("build/classes/java/main/" + relativePath);
    }
}
