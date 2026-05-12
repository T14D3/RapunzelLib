package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * A typed registry for looking up {@link RBlockType} values.
 */
public interface RBlockTypeRegistry extends RTypeRegistry<RBlockType> {
    @Override
    default @NotNull RRegistryKey<RBlockType> registryKey() {
        return RRegistries.BLOCK_TYPES;
    }

    /**
     * Creates a new block type registry backed by the given {@link RRegistryAccess}.
     *
     * @param registries the registry access to use
     * @return the block type registry
     */
    static @NotNull RBlockTypeRegistry of(@NotNull RRegistryAccess registries) {
        return new RBlockTypeRegistry() {
            @Override
            public @NotNull Optional<RBlockType> find(@NotNull RKey key) {
                return registries.find(RRegistries.BLOCK_TYPES, key);
            }

            @Override
            public @NotNull List<RBlockType> entries() {
                return registries.registry(RRegistries.BLOCK_TYPES).entries();
            }
        };
    }
}
