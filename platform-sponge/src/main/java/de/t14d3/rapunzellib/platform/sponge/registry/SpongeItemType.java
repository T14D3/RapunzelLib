package de.t14d3.rapunzellib.platform.sponge.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.item.ItemType;

final class SpongeItemType extends RRegistryTypeHandle<ItemType> implements RItemType {
    SpongeItemType(@NotNull RKey key, @NotNull ItemType handle) {
        super(PlatformId.SPONGE, key, handle);
    }
}
