package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.Objects;

final class SpongeBlockData extends RNativeHandle<BlockState> implements RBlockData {
    SpongeBlockData(BlockState state) {
        super(PlatformId.SPONGE, Objects.requireNonNull(state, "state"));
    }

    @Override
    public @NotNull RRegistryRef<RBlockType> typeRef() {
        return RBlockType.ref(handle().type().key(RegistryTypes.BLOCK_TYPE).asString());
    }

    @Override
    public @NotNull String asString() {
        return handle().asString();
    }
}
