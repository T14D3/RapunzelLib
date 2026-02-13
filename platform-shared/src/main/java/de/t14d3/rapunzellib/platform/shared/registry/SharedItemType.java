package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

final class SharedItemType extends RRegistryTypeHandle<Item> implements RItemType {
    SharedItemType(@NotNull PlatformId platformId, @NotNull RKey key, @NotNull Item handle) {
        super(platformId, key, handle);
    }
}
