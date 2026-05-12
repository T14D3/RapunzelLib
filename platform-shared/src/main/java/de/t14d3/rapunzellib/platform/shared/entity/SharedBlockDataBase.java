package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

/**
 * Abstract base implementation of {@link RBlockData}, wrapping a Minecraft {@link BlockState}.
 * <p>
 * Provides type reference resolution and a string serialization that includes
 * all block state properties (e.g. {@code minecraft:oak_log[axis=y]}).
 * </p>
 */
public abstract class SharedBlockDataBase extends RNativeHandle<BlockState> implements RBlockData {
    /**
     * Constructs a new block data wrapper.
     *
     * @param platformId the platform identifier
     * @param state      the native BlockState to wrap
     */
    protected SharedBlockDataBase(@NotNull PlatformId platformId, @NotNull BlockState state) {
        super(platformId, Objects.requireNonNull(state, "state"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull RRegistryRef<RBlockType> typeRef() {
        return RBlockType.ref(BuiltInRegistries.BLOCK.getKey(handle().getBlock()).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NotNull String asString() {
        String key = typeKey().asString();
        BlockState state = handle();
        Collection<Property<?>> properties = state.getProperties();
        if (properties.isEmpty()) return key;

        StringBuilder out = new StringBuilder(key.length() + 32);
        out.append(key).append('[');
        boolean first = true;
        for (Property<?> property : properties) {
            if (!first) out.append(',');
            first = false;
            out.append(property.getName()).append('=');

            @SuppressWarnings({"rawtypes", "unchecked"})
            Comparable<?> value = state.getValue((Property) property);
            out.append(value);
        }
        return out.append(']').toString();
    }
}
