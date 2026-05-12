package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

/**
 * Shared platform wrapper for a Minecraft {@link Item}, implementing {@link RItemType}.
 * <p>
 * Extends {@link RRegistryTypeHandle} to provide the native handle and key-based identity.
 * </p>
 */
final class SharedItemType extends RRegistryTypeHandle<Item> implements RItemType {
    /**
     * Constructs a new item type wrapper.
     *
     * @param platformId the platform identifier
     * @param key        the registry key for this item type
     * @param handle     the native Minecraft Item handle
     */
    SharedItemType(@NotNull PlatformId platformId, @NotNull RKey key, @NotNull Item handle) {
        super(platformId, key, handle);
    }
}
