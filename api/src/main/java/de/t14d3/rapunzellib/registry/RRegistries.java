package de.t14d3.rapunzellib.registry;

/**
 * Holds the well-known {@link RRegistryKey} constants for standard RapunzelLib registries.
 */
public final class RRegistries {
    /** Registry key for entity types. */
    public static final RRegistryKey<REntityType> ENTITY_TYPES = RRegistryKey.of("rapunzellib:entity_type", REntityType.class);
    /** Registry key for item types. */
    public static final RRegistryKey<RItemType> ITEM_TYPES = RRegistryKey.of("rapunzellib:item_type", RItemType.class);
    /** Registry key for block types. */
    public static final RRegistryKey<RBlockType> BLOCK_TYPES = RRegistryKey.of("rapunzellib:block_type", RBlockType.class);

    private RRegistries() {
    }
}
