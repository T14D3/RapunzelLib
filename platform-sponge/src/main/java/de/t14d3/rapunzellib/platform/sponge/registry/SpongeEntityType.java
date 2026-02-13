package de.t14d3.rapunzellib.platform.sponge.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import org.jetbrains.annotations.NotNull;

final class SpongeEntityType extends RRegistryTypeHandle<org.spongepowered.api.entity.EntityType<?>> implements REntityType {
    SpongeEntityType(@NotNull RKey key, @NotNull org.spongepowered.api.entity.EntityType<?> handle) {
        super(PlatformId.SPONGE, key, handle);
    }
}
