package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.registry.DefaultRRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SharedRegistryBridge {
    private SharedRegistryBridge() {
    }

    public static @NotNull RRegistryAccess createRegistryAccess(@NotNull PlatformId platformId) {
        DefaultRRegistryAccess registries = new DefaultRRegistryAccess();
        register(registries, platformId);
        return registries;
    }

    private static void register(@NotNull DefaultRRegistryAccess registries, @NotNull PlatformId platformId) {
        Objects.requireNonNull(registries, "registries");
        PlatformId resolvedPlatformId = Objects.requireNonNull(platformId, "platformId");

        registries.register(RRegistries.ENTITY_TYPES, new SharedEntityTypeRegistry(resolvedPlatformId));
        registries.register(RRegistries.ITEM_TYPES, new SharedItemTypeRegistry(resolvedPlatformId));
        registries.register(RRegistries.BLOCK_TYPES, new SharedBlockTypeRegistry(resolvedPlatformId));
    }
}
