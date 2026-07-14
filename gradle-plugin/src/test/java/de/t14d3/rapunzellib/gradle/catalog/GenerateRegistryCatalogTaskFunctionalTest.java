package de.t14d3.rapunzellib.gradle.catalog;

import de.t14d3.rapunzellib.gradle.testutil.TestSupport;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateRegistryCatalogTaskFunctionalTest {
    @TempDir
    Path tempDir;

    @Test
    void pluginDslWiresMultiCatalogGenerationThroughAggregateTask() throws Exception {
        Path nativeClasspath = compileBukkitFixture("paper-multi-catalogs");

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
                registryCatalogs {
                    create('demo-item-types') {
                        packageName.set('com.example.catalog')
                        className.set('GeneratedItemCatalog')
                        domainName.set('demo item types')
                        registryValueType.set('de.t14d3.rapunzellib.registry.RItemType')
                        registryKeyOwnerType.set('de.t14d3.rapunzellib.registry.RRegistries')
                        registryKeyFieldName.set('ITEM_TYPES')
                        source {
                            bukkitItemTypes()
                            classpath.from(files('%s'))
                        }
                    }
                    create('demo-block-types') {
                        packageName.set('com.example.catalog')
                        className.set('GeneratedBlockCatalog')
                        domainName.set('demo block types')
                        registryValueType.set('de.t14d3.rapunzellib.registry.RBlockType')
                        registryKeyOwnerType.set('de.t14d3.rapunzellib.registry.RRegistries')
                        registryKeyFieldName.set('BLOCK_TYPES')
                        source {
                            bukkitBlockTypes()
                            classpath.from(files('%s'))
                        }
                    }
                }
            }
            """.formatted(TestSupport.toGradlePath(nativeClasspath), TestSupport.toGradlePath(nativeClasspath))
        );
        writeRegistryFixtureTypes();
        TestSupport.writeFile(
            tempDir,
            "src/main/java/com/example/UsesMultipleCatalogs.java",
            """
            package com.example;

            import com.example.catalog.GeneratedBlockCatalog;
            import com.example.catalog.GeneratedItemCatalog;
            import de.t14d3.rapunzellib.objects.RKey;

            public final class UsesMultipleCatalogs {
                private UsesMultipleCatalogs() {
                }

                public static RKey appleKey() {
                    return GeneratedItemCatalog.Minecraft.APPLE_KEY;
                }

                public static RKey oakLogKey() {
                    return GeneratedBlockCatalog.Minecraft.OAK_LOG_KEY;
                }
            }
            """
        );

        BuildResult result = TestSupport.runGradle(tempDir, "rapunzellibGenerateRegistryCatalogs", "compileJava");

        assertEquals(SUCCESS, result.task(":rapunzellibGenerateRegistryCatalogs").getOutcome());
        assertEquals(SUCCESS, result.task(":compileJava").getOutcome());
        assertTrue(Files.exists(generatedSource("com/example/catalog/GeneratedItemCatalog.java")));
        assertTrue(Files.exists(generatedSource("com/example/catalog/GeneratedBlockCatalog.java")));
        assertTrue(Files.exists(compiledClass("com/example/UsesMultipleCatalogs.class")));
    }

    @Test
    void parityVerificationRunsAgainstConfiguredSources() throws Exception {
        Path canonicalClasspath = compileSharedItemFixture("canonical", Map.of("CUT_STANDSTONE_SLAB", "cut_sandstone_slab"));
        Path candidateClasspath = compileSharedItemFixture("candidate", Map.of("CUT_STANDSTONE_SLAB", "cut_standstone_slab", "APPLE", "apple"));

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
                registryCatalogs {
                    create('vanilla-item-types') {
                        packageName.set('com.example.catalog')
                        className.set('GeneratedSharedItems')
                        domainName.set('shared item types')
                        registryValueType.set('de.t14d3.rapunzellib.registry.RItemType')
                        registryKeyOwnerType.set('de.t14d3.rapunzellib.registry.RRegistries')
                        registryKeyFieldName.set('ITEM_TYPES')
                        source {
                            mojangItemTypes()
                            classpath.from(files('%s'))
                        }
                        verifyAgainst('candidate') {
                            mojangItemTypes()
                            normalizationProfile.set('%s')
                            allowSupersetOfCanonical.set(true)
                            classpath.from(files('%s'))
                        }
                    }
                }
            }
            """.formatted(
                TestSupport.toGradlePath(canonicalClasspath),
                RegistryCatalogNormalizationProfile.VANILLA_MOJANG_ITEM_TYPES,
                TestSupport.toGradlePath(candidateClasspath)
            )
        );
        writeRegistryFixtureTypes();

        BuildResult result = TestSupport.runGradle(tempDir, "rapunzellibVerifyRegistryCatalogParity", "compileJava");

        assertEquals(SUCCESS, result.task(":rapunzellibVerifyRegistryCatalogParity").getOutcome());
        assertEquals(SUCCESS, result.task(":compileJava").getOutcome());
    }

    private void writeConsumerProject(String buildFile) {
        TestSupport.writeFile(tempDir, "settings.gradle", "rootProject.name = 'consumer-registry-catalog'");
        TestSupport.writeFile(tempDir, "build.gradle", buildFile.strip());
    }

    private void writeRegistryFixtureTypes() {
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
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/registry/RRegistryKey.java",
            """
            package de.t14d3.rapunzellib.registry;

            import de.t14d3.rapunzellib.objects.RKey;

            public final class RRegistryKey<T> {
                public RRegistryRef<T> ref(RKey key) {
                    return new RRegistryRef<>();
                }

                public RRegistryRef<T> ref(String key) {
                    return new RRegistryRef<>();
                }
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/registry/RRegistryRef.java",
            """
            package de.t14d3.rapunzellib.registry;

            public final class RRegistryRef<T> {
            }
            """
        );
        TestSupport.writeFile(
            tempDir,
            "src/main/java/de/t14d3/rapunzellib/registry/RRegistries.java",
            """
            package de.t14d3.rapunzellib.registry;

            public final class RRegistries {
                public static final RRegistryKey<RItemType> ITEM_TYPES = new RRegistryKey<>();
                public static final RRegistryKey<RBlockType> BLOCK_TYPES = new RRegistryKey<>();

                private RRegistries() {
                }
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
                public static Optional<RItemType> find(RKey key) {
                    return Optional.of(new RItemType());
                }

                public static RItemType require(RKey key) {
                    return new RItemType();
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
            import java.util.Optional;

            public final class RBlockType {
                public static Optional<RBlockType> find(RKey key) {
                    return Optional.of(new RBlockType());
                }

                public static RBlockType require(RKey key) {
                    return new RBlockType();
                }
            }
            """
        );
    }

    private Path compileBukkitFixture(String name) {
        return TestSupport.compileSources(
            tempDir,
            name,
            Map.of(
                "org/bukkit/NamespacedKey.java",
                """
                package org.bukkit;

                public record NamespacedKey(String namespace, String key) {
                    @Override
                    public String toString() {
                        return this.namespace + ":" + this.key;
                    }
                }
                """,
                "org/bukkit/inventory/ItemType.java",
                """
                package org.bukkit.inventory;

                import org.bukkit.NamespacedKey;

                public final class ItemType {
                    public static final ItemType APPLE = new ItemType("apple");
                    public static final ItemType STONE = new ItemType("stone");

                    private final NamespacedKey key;

                    private ItemType(String key) {
                        this.key = new NamespacedKey("minecraft", key);
                    }

                    public NamespacedKey getKey() {
                        return this.key;
                    }
                }
                """,
                "org/bukkit/block/BlockType.java",
                """
                package org.bukkit.block;

                import org.bukkit.NamespacedKey;

                public final class BlockType {
                    public static final BlockType OAK_LOG = new BlockType("oak_log");
                    public static final BlockType STONE = new BlockType("stone");

                    private final NamespacedKey key;

                    private BlockType(String key) {
                        this.key = new NamespacedKey("minecraft", key);
                    }

                    public NamespacedKey getKey() {
                        return this.key;
                    }
                }
                """
            )
        );
    }

    private Path compileSharedItemFixture(String name, Map<String, String> items) {
        StringBuilder itemFields = new StringBuilder();
        for (Map.Entry<String, String> entry : items.entrySet()) {
            itemFields.append("    public static final Item ").append(entry.getKey()).append(" = new Item(\"").append(entry.getValue()).append("\");\n");
        }
        return TestSupport.compileSources(
            tempDir,
            name,
            Map.of(
                "net/minecraft/resources/Identifier.java",
                """
                package net.minecraft.resources;

                public record Identifier(String namespace, String path) {
                    @Override
                    public String toString() {
                        return this.namespace + ":" + this.path;
                    }
                }
                """,
                "net/minecraft/resources/ResourceKey.java",
                """
                package net.minecraft.resources;

                public record ResourceKey<T>(Identifier location) {
                }
                """,
                "net/minecraft/core/Holder.java",
                """
                package net.minecraft.core;

                import net.minecraft.resources.ResourceKey;

                public interface Holder<T> {
                    final class Reference<T> {
                        private final ResourceKey<T> key;

                        public Reference(ResourceKey<T> key) {
                            this.key = key;
                        }

                        public ResourceKey<T> key() {
                            return this.key;
                        }
                    }
                }
                """,
                "net/minecraft/world/item/Item.java",
                """
                package net.minecraft.world.item;

                import net.minecraft.core.Holder;
                import net.minecraft.resources.Identifier;
                import net.minecraft.resources.ResourceKey;

                public class Item {
                    private final Holder.Reference<Item> holder;

                    public Item(String key) {
                        this.holder = new Holder.Reference<>(new ResourceKey<>(new Identifier("minecraft", key)));
                    }

                    public Holder.Reference<Item> builtInRegistryHolder() {
                        return this.holder;
                    }
                }
                """,
                "net/minecraft/world/item/Items.java",
                """
                package net.minecraft.world.item;

                public final class Items {
                %s
                    private Items() {
                    }
                }
                """.formatted(itemFields)
            )
        );
    }

    private Path generatedSource(String relativePath) {
        return tempDir.resolve("src/generated/java/" + relativePath);
    }

    private Path compiledClass(String relativePath) {
        return tempDir.resolve("build/classes/java/main/" + relativePath);
    }
}
