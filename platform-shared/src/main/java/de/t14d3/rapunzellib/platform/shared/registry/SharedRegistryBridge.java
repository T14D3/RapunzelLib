package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.registry.DefaultRRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Bridge factory that creates and populates a {@link RRegistryAccess} with shared platform registries.
 * <p>
 * Registers entity type, item type, and block type registries backed by Minecraft's
 * {@code BuiltInRegistries}.
 * </p>
 */
public final class SharedRegistryBridge {
    private SharedRegistryBridge() {
    }

    /**
     * Creates a fully-populated {@link RRegistryAccess} containing all shared platform registries.
     *
     * @param platformId the platform identifier
     * @return a new registry access instance with all registries registered
     */
    public static @NotNull RRegistryAccess createRegistryAccess(@NotNull PlatformId platformId) {
        DefaultRRegistryAccess registries = new DefaultRRegistryAccess();
        register(registries, platformId);
        return registries;
    }

    /**
     * Registers all shared platform registries into the given access object.
     *
     * @param registries the registry access to populate
     * @param platformId the platform identifier to pass to each registry
     */
    private static void register(@NotNull DefaultRRegistryAccess registries, @NotNull PlatformId platformId) {
        Objects.requireNonNull(registries, "registries");
        PlatformId resolvedPlatformId = Objects.requireNonNull(platformId, "platformId");

        registries.register(RRegistries.ENTITY_TYPES, new SharedEntityTypeRegistry(resolvedPlatformId));
        registries.register(RRegistries.ITEM_TYPES, new SharedItemTypeRegistry(resolvedPlatformId));
        registries.register(RRegistries.BLOCK_TYPES, new SharedBlockTypeRegistry(resolvedPlatformId));
    }
}
