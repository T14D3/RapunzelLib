package de.t14d3.rapunzellib.gradle.catalog;

import de.t14d3.rapunzellib.gradle.RegistryCatalogSourceType;
import de.t14d3.rapunzellib.gradle.testutil.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryCatalogSourceExtractorTest {
    private final String mojangKeyAccessor = "builtInRegistryHolder.key.identifier|builtInRegistryHolder.key.location";

    @TempDir
    Path tempDir;

    @Test
    void reflectedBukkitExtractionUsesNativeKeys() {
        Path classpath = TestSupport.compileSources(
            tempDir,
            "bukkit-native",
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
                "org/bukkit/entity/EntityType.java",
                """
                package org.bukkit.entity;

                import org.bukkit.NamespacedKey;

                public enum EntityType {
                    UNKNOWN("unknown"),
                    ZOMBIE("zombie"),
                    BLAZE("blaze");

                    private final NamespacedKey key;

                    EntityType(String key) {
                        this.key = new NamespacedKey("minecraft", key);
                    }

                    public NamespacedKey getKey() {
                        return this.key;
                    }
                }
                """
            )
        );

        ExtractedRegistryCatalog items = RegistryCatalogSourceExtractor.extract(
            RegistryCatalogSourceType.NATIVE_STATIC_FIELDS,
            List.of(classpath.toFile()),
            RegistryCatalogNormalizationProfile.NONE,
            "",
            "org.bukkit.inventory.ItemType",
            "org.bukkit.inventory.ItemType",
            List.of(),
            List.of(),
            "getKey",
            Set.of()
        );
        ExtractedRegistryCatalog entities = RegistryCatalogSourceExtractor.extract(
            RegistryCatalogSourceType.NATIVE_ENUM,
            List.of(classpath.toFile()),
            RegistryCatalogNormalizationProfile.NONE,
            "org.bukkit.entity.EntityType",
            "",
            "",
            List.of(),
            List.of(),
            "getKey",
            Set.of("UNKNOWN")
        );

        assertTrue(items.keys().stream().anyMatch(entry -> entry.value().equals("minecraft:apple")));
        assertTrue(items.keys().stream().anyMatch(entry -> entry.value().equals("minecraft:stone")));
        assertEquals(items.keys().stream().map(NamespacedKeyEntry::value).sorted().toList(), items.keys().stream().map(NamespacedKeyEntry::value).toList());

        assertTrue(entities.keys().stream().anyMatch(entry -> entry.value().equals("minecraft:zombie")));
        assertTrue(entities.keys().stream().noneMatch(entry -> entry.value().equals("minecraft:unknown")));
    }

    @Test
    void reflectedSharedExtractionPrefersNativeHolderKeysOverSymbols() {
        Path classpath = TestSupport.compileSources(
            tempDir,
            "shared-native",
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
                    public static final Item CUT_STANDSTONE_SLAB = new Item("cut_sandstone_slab");
                    public static final Item DRY_SHORT_GRASS = new Item("short_dry_grass");

                    private Items() {
                    }
                }
                """
            )
        );

        ExtractedRegistryCatalog extracted = RegistryCatalogSourceExtractor.extract(
            RegistryCatalogSourceType.NATIVE_STATIC_FIELDS,
            List.of(classpath.toFile()),
            RegistryCatalogNormalizationProfile.NONE,
            "",
            "net.minecraft.world.item.Items",
            "net.minecraft.world.item.Item",
            List.of(),
            List.of(),
            mojangKeyAccessor,
            Set.of()
        );

        assertEquals(
            List.of("minecraft:cut_sandstone_slab", "minecraft:short_dry_grass"),
            extracted.keys().stream().map(NamespacedKeyEntry::value).toList()
        );
    }

    @Test
    void parityVerifierHonorsNormalizationAndAllowedSupersets() {
        Path canonicalClasspath = TestSupport.compileSources(
            tempDir,
            "canonical",
            sharedItemFixtureSources(Map.of("CUT_STANDSTONE_SLAB", "cut_sandstone_slab"))
        );
        Path candidateClasspath = TestSupport.compileSources(
            tempDir,
            "candidate",
            sharedItemFixtureSources(Map.of("CUT_STANDSTONE_SLAB", "cut_standstone_slab", "APPLE", "apple"))
        );

        RegistryCatalogParityResult result = RegistryCatalogParityVerifier.verify(
            "vanilla item types",
            List.of(
                new RegistryCatalogSourceDefinition(
                    "canonical",
                    RegistryCatalogSourceType.NATIVE_STATIC_FIELDS,
                    List.of(canonicalClasspath.toFile()),
                    RegistryCatalogNormalizationProfile.NONE,
                    false,
                    "",
                    "net.minecraft.world.item.Items",
                    "net.minecraft.world.item.Item",
                    List.of(),
                    List.of(),
                    mojangKeyAccessor,
                    Set.of()
                ),
                new RegistryCatalogSourceDefinition(
                    "candidate",
                    RegistryCatalogSourceType.NATIVE_STATIC_FIELDS,
                    List.of(candidateClasspath.toFile()),
                    RegistryCatalogNormalizationProfile.VANILLA_MOJANG_ITEM_TYPES,
                    true,
                    "",
                    "net.minecraft.world.item.Items",
                    "net.minecraft.world.item.Item",
                    List.of(),
                    List.of(),
                    mojangKeyAccessor,
                    Set.of()
                )
            )
        );

        assertEquals(1, result.entryCount());
        assertEquals("candidate", result.comparedSources().getFirst().name());
    }

    private Map<String, String> sharedItemFixtureSources(Map<String, String> items) {
        StringBuilder itemFields = new StringBuilder();
        for (Map.Entry<String, String> entry : items.entrySet()) {
            itemFields.append("    public static final Item ").append(entry.getKey()).append(" = new Item(\"").append(entry.getValue()).append("\");\n");
        }
        return Map.of(
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
        );
    }
}
