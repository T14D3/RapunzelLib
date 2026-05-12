package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistries;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import org.jetbrains.annotations.NotNull;

/**
 * A block type registry backed by an {@link RRegistryAccess} instance.
 * <p>
 * Delegates all operations to the underlying registry access for
 * {@link RRegistries#BLOCK_TYPES}.
 */
public final class RegistryAccessBackedBlockTypeRegistry extends RegistryAccessBackedRegistry<RBlockType>
    implements RBlockTypeRegistry {

    /**
     * Creates a new block type registry backed by the given registry access.
     *
     * @param registries the registry access to delegate to
     */
    public RegistryAccessBackedBlockTypeRegistry(@NotNull RRegistryAccess registries) {
        super(registries, RRegistries.BLOCK_TYPES);
    }
}
