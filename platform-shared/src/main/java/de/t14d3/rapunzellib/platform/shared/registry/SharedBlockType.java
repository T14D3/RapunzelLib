package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Shared platform wrapper for a Minecraft {@link Block}, implementing {@link RBlockType}.
 * <p>
 * Extends {@link RRegistryTypeHandle} to provide the native handle and key-based identity.
 * </p>
 */
final class SharedBlockType extends RRegistryTypeHandle<Block> implements RBlockType {
    /**
     * Constructs a new block type wrapper.
     *
     * @param platformId the platform identifier
     * @param key        the registry key for this block type
     * @param handle     the native Minecraft Block handle
     */
    SharedBlockType(@NotNull PlatformId platformId, @NotNull RKey key, @NotNull Block handle) {
        super(platformId, key, handle);
    }
}
