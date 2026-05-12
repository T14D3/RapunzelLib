package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.registry.AbstractTypeRegistry;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Shared platform implementation of {@link RItemTypeRegistry} backed by Minecraft's {@link BuiltInRegistries#ITEM}.
 * <p>
 * Wraps native {@link Item} handles into {@link SharedItemType} wrappers, delegating to
 * {@link AbstractTypeRegistry} for caching and lookup logic.
 * </p>
 */
public final class SharedItemTypeRegistry extends AbstractTypeRegistry<Item, SharedItemType, RItemType> implements RItemTypeRegistry {
    private final PlatformId platformId;

    /**
     * Constructs a new item type registry for the given platform.
     *
     * @param platformId the platform identifier
     */
    public SharedItemTypeRegistry(@NotNull PlatformId platformId) {
        super(
            // #if VERSION >= 1.21.11
            requestedKey -> BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(requestedKey.namespace(), requestedKey.path())),
            // #else
            requestedKey -> BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(requestedKey.namespace(), requestedKey.path())),
            // #endif
            () -> BuiltInRegistries.ITEM,
            handle -> RKey.of(BuiltInRegistries.ITEM.getKey(handle).toString()),
            RItemType.class
        );
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSameHandle(@NotNull Item existingHandle, @NotNull Item newHandle) {
        return existingHandle == newHandle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull SharedItemType createWrapper(@NotNull RKey key, @NotNull Item handle) {
        return new SharedItemType(platformId, key, handle);
    }
}
