package de.t14d3.rapunzellib.platform.sponge.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.registry.AbstractTypeRegistry;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.Objects;

public final class SpongeItemTypeRegistry extends AbstractTypeRegistry<ItemType, SpongeItemType, RItemType> implements RItemTypeRegistry {
    public SpongeItemTypeRegistry(@NotNull Server server) {
        super(
            requestedKey -> Objects.requireNonNull(server, "server")
                .registry(RegistryTypes.ITEM_TYPE)
                .findValue(ResourceKey.resolve(requestedKey.asString()))
                .orElse(null),
            () -> Objects.requireNonNull(server, "server").registry(RegistryTypes.ITEM_TYPE).streamEntries().map(entry -> entry.value()).toList(),
            handle -> RKey.of(handle.key(RegistryTypes.ITEM_TYPE).asString()),
            RItemType.class
        );
    }

    @Override
    protected @NotNull SpongeItemType createWrapper(@NotNull RKey key, @NotNull ItemType handle) {
        return new SpongeItemType(key, handle);
    }
}
