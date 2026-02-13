package de.t14d3.rapunzellib.gradle.catalog;

import org.gradle.api.GradleException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class RegistryCatalogKeyNormalizer {
    private static final Map<String, String> MOJANG_ITEM_ALIASES = Map.of(
        "minecraft:cut_standstone_slab", "minecraft:cut_sandstone_slab",
        "minecraft:dry_short_grass", "minecraft:short_dry_grass",
        "minecraft:dry_tall_grass", "minecraft:tall_dry_grass"
    );

    private static final Map<String, String> PAPER_BLOCK_ALIASES = Map.of(
        "minecraft:potted_azalea", "minecraft:potted_azalea_bush",
        "minecraft:potted_flowering_azalea", "minecraft:potted_flowering_azalea_bush"
    );

    private RegistryCatalogKeyNormalizer() {
    }

    public static List<NamespacedKeyEntry> normalize(String profile, List<NamespacedKeyEntry> keys) {
        Map<String, String> aliases = aliasesFor(profile);
        List<NamespacedKeyEntry> normalized = new ArrayList<>();
        for (NamespacedKeyEntry entry : keys) {
            String aliased = aliases.get(entry.value());
            normalized.add(aliased != null ? parseNamespacedKey(aliased) : entry);
        }
        return normalized.stream()
            .distinct()
            .sorted(Comparator.comparing(NamespacedKeyEntry::namespace).thenComparing(NamespacedKeyEntry::path))
            .toList();
    }

    public static String describe(String profile) {
        return switch (profile) {
            case RegistryCatalogNormalizationProfile.NONE -> null;
            case RegistryCatalogNormalizationProfile.VANILLA_MOJANG_ITEM_TYPES ->
                "normalized for vanilla Shared item aliases";
            case RegistryCatalogNormalizationProfile.VANILLA_PAPER_BLOCK_TYPES ->
                "normalized for vanilla Paper block aliases";
            default -> throw new GradleException("Unsupported registry catalog normalization profile '" + profile + "'.");
        };
    }

    private static Map<String, String> aliasesFor(String profile) {
        return switch (profile) {
            case RegistryCatalogNormalizationProfile.NONE -> Map.of();
            case RegistryCatalogNormalizationProfile.VANILLA_MOJANG_ITEM_TYPES -> MOJANG_ITEM_ALIASES;
            case RegistryCatalogNormalizationProfile.VANILLA_PAPER_BLOCK_TYPES -> PAPER_BLOCK_ALIASES;
            default -> throw new GradleException("Unsupported registry catalog normalization profile '" + profile + "'.");
        };
    }

    private static NamespacedKeyEntry parseNamespacedKey(String value) {
        int separatorIndex = value.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == value.length() - 1) {
            throw new GradleException("Invalid normalized registry catalog key '" + value + "'.");
        }
        return new NamespacedKeyEntry(value.substring(0, separatorIndex), value.substring(separatorIndex + 1));
    }
}
