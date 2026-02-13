package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistries;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import org.jetbrains.annotations.NotNull;

public final class RegistryAccessBackedEntityTypeRegistry extends RegistryAccessBackedRegistry<REntityType>
    implements REntityTypeRegistry {

    public RegistryAccessBackedEntityTypeRegistry(@NotNull RRegistryAccess registries) {
        super(registries, RRegistries.ENTITY_TYPES);
    }
}
