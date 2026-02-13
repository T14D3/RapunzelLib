package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.registry.AbstractTypeRegistry;
import de.t14d3.rapunzellib.registry.RItemType;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SharedItemTypeRegistry extends AbstractTypeRegistry<Item, SharedItemType, RItemType> implements RItemTypeRegistry {
    private final PlatformId platformId;

    public SharedItemTypeRegistry(@NotNull PlatformId platformId) {
        super(
            requestedKey -> BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(requestedKey.namespace(), requestedKey.path())),
            () -> BuiltInRegistries.ITEM,
            handle -> RKey.of(BuiltInRegistries.ITEM.getKey(handle).toString()),
            RItemType.class
        );
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    @Override
    protected boolean isSameHandle(@NotNull Item existingHandle, @NotNull Item newHandle) {
        return existingHandle == newHandle;
    }

    @Override
    protected @NotNull SharedItemType createWrapper(@NotNull RKey key, @NotNull Item handle) {
        return new SharedItemType(platformId, key, handle);
    }
}
