package de.t14d3.rapunzellib.platform.sponge.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.registry.AbstractTypeRegistry;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.Objects;

public final class SpongeEntityTypeRegistry
    extends AbstractTypeRegistry<org.spongepowered.api.entity.EntityType<?>, SpongeEntityType, REntityType>
    implements REntityTypeRegistry {

    public SpongeEntityTypeRegistry(@NotNull Server server) {
        super(
            requestedKey -> Objects.requireNonNull(server, "server")
                .registry(RegistryTypes.ENTITY_TYPE)
                .findValue(ResourceKey.resolve(requestedKey.asString()))
                .orElse(null),
            () -> Objects.requireNonNull(server, "server").registry(RegistryTypes.ENTITY_TYPE).streamEntries().map(entry -> entry.value()).toList(),
            handle -> RKey.of(handle.key(RegistryTypes.ENTITY_TYPE).asString()),
            REntityType.class
        );
    }

    @Override
    protected @NotNull SpongeEntityType createWrapper(@NotNull RKey key, @NotNull org.spongepowered.api.entity.EntityType<?> handle) {
        return new SpongeEntityType(key, handle);
    }
}
