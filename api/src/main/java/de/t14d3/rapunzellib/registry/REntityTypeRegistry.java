package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * A typed registry for looking up {@link REntityType} values.
 */
public interface REntityTypeRegistry extends RTypeRegistry<REntityType> {
    @Override
    default @NotNull RRegistryKey<REntityType> registryKey() {
        return RRegistries.ENTITY_TYPES;
    }

    /**
     * Creates a new entity type registry backed by the given {@link RRegistryAccess}.
     *
     * @param registries the registry access to use
     * @return the entity type registry
     */
    static @NotNull REntityTypeRegistry of(@NotNull RRegistryAccess registries) {
        return new REntityTypeRegistry() {
            @Override
            public @NotNull Optional<REntityType> find(@NotNull RKey key) {
                return registries.find(RRegistries.ENTITY_TYPES, key);
            }

            @Override
            public @NotNull List<REntityType> entries() {
                return registries.registry(RRegistries.ENTITY_TYPES).entries();
            }
        };
    }
}
