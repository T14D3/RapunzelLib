package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.registry.AbstractTypeRegistry;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SharedBlockTypeRegistry extends AbstractTypeRegistry<Block, SharedBlockType, RBlockType> implements RBlockTypeRegistry {
    private final PlatformId platformId;

    public SharedBlockTypeRegistry(@NotNull PlatformId platformId) {
        super(
            requestedKey -> BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(requestedKey.namespace(), requestedKey.path())),
            () -> BuiltInRegistries.BLOCK,
            handle -> RKey.of(BuiltInRegistries.BLOCK.getKey(handle).toString()),
            RBlockType.class
        );
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    @Override
    protected boolean isSameHandle(@NotNull Block existingHandle, @NotNull Block newHandle) {
        return existingHandle == newHandle;
    }

    @Override
    protected @NotNull SharedBlockType createWrapper(@NotNull RKey key, @NotNull Block handle) {
        return new SharedBlockType(platformId, key, handle);
    }
}
