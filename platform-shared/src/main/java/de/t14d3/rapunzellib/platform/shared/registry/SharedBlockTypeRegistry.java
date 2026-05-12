package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.registry.AbstractTypeRegistry;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Shared platform implementation of {@link RBlockTypeRegistry} backed by Minecraft's {@link BuiltInRegistries#BLOCK}.
 * <p>
 * Wraps native {@link Block} handles into {@link SharedBlockType} wrappers, delegating to
 * {@link AbstractTypeRegistry} for caching and lookup logic.
 * </p>
 */
public final class SharedBlockTypeRegistry extends AbstractTypeRegistry<Block, SharedBlockType, RBlockType> implements RBlockTypeRegistry {
    private final PlatformId platformId;

    /**
     * Constructs a new block type registry for the given platform.
     *
     * @param platformId the platform identifier
     */
    public SharedBlockTypeRegistry(@NotNull PlatformId platformId) {
        super(
            // #if VERSION >= 1.21.11
            requestedKey -> BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(requestedKey.namespace(), requestedKey.path())),
            // #else
            requestedKey -> BuiltInRegistries.BLOCK.getValue(ResourceLocation.fromNamespaceAndPath(requestedKey.namespace(), requestedKey.path())),
            // #endif
            () -> BuiltInRegistries.BLOCK,
            handle -> RKey.of(BuiltInRegistries.BLOCK.getKey(handle).toString()),
            RBlockType.class
        );
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSameHandle(@NotNull Block existingHandle, @NotNull Block newHandle) {
        return existingHandle == newHandle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull SharedBlockType createWrapper(@NotNull RKey key, @NotNull Block handle) {
        return new SharedBlockType(platformId, key, handle);
    }
}
