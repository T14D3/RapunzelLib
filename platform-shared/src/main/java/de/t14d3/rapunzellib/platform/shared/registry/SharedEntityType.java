package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * Shared platform wrapper for a Minecraft {@link EntityType}, implementing {@link REntityType}.
 * <p>
 * Extends {@link RRegistryTypeHandle} to provide the native handle and key-based identity.
 * </p>
 */
final class SharedEntityType extends RRegistryTypeHandle<EntityType<?>> implements REntityType {
    /**
     * Constructs a new entity type wrapper.
     *
     * @param platformId the platform identifier
     * @param key        the registry key for this entity type
     * @param handle     the native Minecraft EntityType handle
     */
    SharedEntityType(@NotNull PlatformId platformId, @NotNull RKey key, @NotNull EntityType<?> handle) {
        super(platformId, key, handle);
    }
}
