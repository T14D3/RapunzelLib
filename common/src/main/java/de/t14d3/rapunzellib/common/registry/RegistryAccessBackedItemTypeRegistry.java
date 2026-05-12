package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistries;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import org.jetbrains.annotations.NotNull;

/**
 * An item type registry backed by an {@link RRegistryAccess} instance.
 * <p>
 * Delegates all operations to the underlying registry access for
 * {@link RRegistries#ITEM_TYPES}.
 */
public final class RegistryAccessBackedItemTypeRegistry extends RegistryAccessBackedRegistry<RItemType>
    implements RItemTypeRegistry {

    /**
     * Creates a new item type registry backed by the given registry access.
     *
     * @param registries the registry access to delegate to
     */
    public RegistryAccessBackedItemTypeRegistry(@NotNull RRegistryAccess registries) {
        super(registries, RRegistries.ITEM_TYPES);
    }
}
