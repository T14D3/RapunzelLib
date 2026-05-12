package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistries;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import org.jetbrains.annotations.NotNull;

/**
 * An entity type registry backed by an {@link RRegistryAccess} instance.
 * <p>
 * Delegates all operations to the underlying registry access for
 * {@link RRegistries#ENTITY_TYPES}.
 */
public final class RegistryAccessBackedEntityTypeRegistry extends RegistryAccessBackedRegistry<REntityType>
    implements REntityTypeRegistry {

    /**
     * Creates a new entity type registry backed by the given registry access.
     *
     * @param registries the registry access to delegate to
     */
    public RegistryAccessBackedEntityTypeRegistry(@NotNull RRegistryAccess registries) {
        super(registries, RRegistries.ENTITY_TYPES);
    }
}
