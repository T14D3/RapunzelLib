package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.Objects;

final class FabricBlockData extends RNativeHandle<BlockState> implements RBlockData {
    FabricBlockData(BlockState state) {
        super(PlatformId.FABRIC, Objects.requireNonNull(state, "state"));
    }

    @Override
    public String typeKey() {
        return BuiltInRegistries.BLOCK.getKey(handle().getBlock()).toString();
    }

    @Override
    public String asString() {
        String key = typeKey();
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

