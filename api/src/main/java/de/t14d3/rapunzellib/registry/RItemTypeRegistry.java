package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.objects.RKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface RItemTypeRegistry extends RTypeRegistry<RItemType> {
    @Override
    default @NotNull RRegistryKey<RItemType> registryKey() {
        return RRegistries.ITEM_TYPES;
    }

    static @NotNull RItemTypeRegistry of(@NotNull RRegistryAccess registries) {
        return new RItemTypeRegistry() {
            @Override
            public @NotNull Optional<RItemType> find(@NotNull RKey key) {
                return registries.find(RRegistries.ITEM_TYPES, key);
            }

            @Override
            public @NotNull List<RItemType> entries() {
                return registries.registry(RRegistries.ITEM_TYPES).entries();
            }
        };
    }
}
