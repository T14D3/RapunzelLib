package de.t14d3.rapunzellib.platform.sponge.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.registry.AbstractTypeRegistry;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.Objects;

public final class SpongeBlockTypeRegistry extends AbstractTypeRegistry<BlockType, SpongeBlockType, RBlockType> implements RBlockTypeRegistry {
    public SpongeBlockTypeRegistry(@NotNull Server server) {
        super(
            requestedKey -> Objects.requireNonNull(server, "server")
                .registry(RegistryTypes.BLOCK_TYPE)
                .findValue(ResourceKey.resolve(requestedKey.asString()))
                .orElse(null),
            () -> Objects.requireNonNull(server, "server").registry(RegistryTypes.BLOCK_TYPE).streamEntries().map(entry -> entry.value()).toList(),
            handle -> RKey.of(handle.key(RegistryTypes.BLOCK_TYPE).asString()),
            RBlockType.class
        );
    }

    @Override
    protected @NotNull SpongeBlockType createWrapper(@NotNull RKey key, @NotNull BlockType handle) {
        return new SpongeBlockType(key, handle);
    }
}
