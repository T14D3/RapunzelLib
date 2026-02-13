package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistries;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import org.jetbrains.annotations.NotNull;

public final class RegistryAccessBackedItemTypeRegistry extends RegistryAccessBackedRegistry<RItemType>
    implements RItemTypeRegistry {

    public RegistryAccessBackedItemTypeRegistry(@NotNull RRegistryAccess registries) {
        super(registries, RRegistries.ITEM_TYPES);
    }
}
