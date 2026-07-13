package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.registry.AbstractTypeRegistry;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Shared platform implementation of {@link REntityTypeRegistry} backed by Minecraft's {@link BuiltInRegistries#ENTITY_TYPE}. */
public final class SharedEntityTypeRegistry
    extends AbstractTypeRegistry<EntityType<?>, SharedEntityType, REntityType>
    implements REntityTypeRegistry {

    private final PlatformId platformId;

    public SharedEntityTypeRegistry(@NotNull PlatformId platformId) {
        super(
            // #if VERSION >= 1.21.11
            requestedKey -> BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath(requestedKey.namespace(), requestedKey.path())),
            // #else
            requestedKey -> BuiltInRegistries.ENTITY_TYPE.getValue(ResourceLocation.fromNamespaceAndPath(requestedKey.namespace(), requestedKey.path())),
            // #endif
            () -> BuiltInRegistries.ENTITY_TYPE,
            handle -> RKey.of(BuiltInRegistries.ENTITY_TYPE.getKey(handle).toString()),
            REntityType.class
        );
        this.platformId = Objects.requireNonNull(platformId, "platformId");
    }

    @Override
    protected boolean isSameHandle(@NotNull EntityType<?> existingHandle, @NotNull EntityType<?> newHandle) {
        return existingHandle == newHandle;
    }

    @Override
    protected @NotNull SharedEntityType createWrapper(@NotNull RKey key, @NotNull EntityType<?> handle) {
        return new SharedEntityType(platformId, key, handle);
    }
}
