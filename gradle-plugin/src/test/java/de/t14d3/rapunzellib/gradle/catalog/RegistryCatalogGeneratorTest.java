package de.t14d3.rapunzellib.gradle.catalog;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryCatalogGeneratorTest {
    @Test
    void renderJavaSourceEmitsTypedKeyAndRefConstants() {
        String source = RegistryCatalogGenerator.renderJavaSource(
            "com.example.registry",
            "VanillaItemTypes",
            "vanilla item types",
            "org.bukkit.inventory.ItemType native static fields assignable to org.bukkit.inventory.ItemType",
            List.of(new NamespacedKeyEntry("minecraft", "stone")),
            "de.t14d3.rapunzellib.registry.RItemType",
            "de.t14d3.rapunzellib.registry.RRegistries",
            "ITEM_TYPES"
        );

        assertTrue(source.contains("import de.t14d3.rapunzellib.registry.RItemType;"));
        assertTrue(source.contains("public static final RRegistryKey<RItemType> REGISTRY = RRegistries.ITEM_TYPES;"));
        assertTrue(source.contains("public static final RKey STONE_KEY = RKey.of(\"minecraft:stone\");"));
        assertTrue(source.contains("public static final RRegistryRef<RItemType> STONE = ref(STONE_KEY);"));
        assertTrue(source.contains("Source: org.bukkit.inventory.ItemType native static fields assignable to org.bukkit.inventory.ItemType."));
        assertTrue(source.contains("public static Optional<RItemType> find(RKey key) {"));
        assertTrue(source.contains("return RItemType.require(key);"));
    }

    @Test
    void renderJavaSourceRejectsBlankRegistryFieldNames() {
        GradleException failure = assertThrows(
            GradleException.class,
            () -> RegistryCatalogGenerator.renderJavaSource(
                "com.example.registry",
                "VanillaItemTypes",
                "vanilla item types",
                "net.minecraft.world.item.Items native static fields",
                List.of(new NamespacedKeyEntry("minecraft", "stone")),
                "de.t14d3.rapunzellib.registry.RItemType",
                "de.t14d3.rapunzellib.registry.RRegistries",
                ""
            )
        );

        assertTrue(failure.getMessage().contains("Registry catalog registry key field name must not be blank."));
    }
}
