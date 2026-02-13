package de.t14d3.rapunzellib.registry;

public final class RRegistries {
    public static final RRegistryKey<REntityType> ENTITY_TYPES = RRegistryKey.of("rapunzellib:entity_type", REntityType.class);
    public static final RRegistryKey<RItemType> ITEM_TYPES = RRegistryKey.of("rapunzellib:item_type", RItemType.class);
    public static final RRegistryKey<RBlockType> BLOCK_TYPES = RRegistryKey.of("rapunzellib:block_type", RBlockType.class);

    private RRegistries() {
    }
}
