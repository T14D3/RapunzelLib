package de.t14d3.rapunzellib.platform.sponge.registry;

import de.t14d3.rapunzellib.common.registry.DefaultRRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.registry.RRegistries;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Server;

import java.util.Objects;

public final class SpongeRegistryBridge {
    private SpongeRegistryBridge() {
    }

    public static @NotNull RRegistryAccess createRegistryAccess(@NotNull Server server) {
        DefaultRRegistryAccess registries = new DefaultRRegistryAccess();
        register(registries, server);
        return registries;
    }

    private static void register(@NotNull DefaultRRegistryAccess registries, @NotNull Server server) {
        Objects.requireNonNull(registries, "registries");
        Server resolvedServer = Objects.requireNonNull(server, "server");

        registries.register(RRegistries.ENTITY_TYPES, new SpongeEntityTypeRegistry(resolvedServer));
        registries.register(RRegistries.ITEM_TYPES, new SpongeItemTypeRegistry(resolvedServer));
        registries.register(RRegistries.BLOCK_TYPES, new SpongeBlockTypeRegistry(resolvedServer));
    }
}
