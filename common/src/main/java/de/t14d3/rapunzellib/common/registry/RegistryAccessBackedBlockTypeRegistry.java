package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistries;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import org.jetbrains.annotations.NotNull;

public final class RegistryAccessBackedBlockTypeRegistry extends RegistryAccessBackedRegistry<RBlockType>
    implements RBlockTypeRegistry {

    public RegistryAccessBackedBlockTypeRegistry(@NotNull RRegistryAccess registries) {
        super(registries, RRegistries.BLOCK_TYPES);
    }
}
