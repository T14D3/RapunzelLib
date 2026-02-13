package de.t14d3.rapunzellib.platform.sponge.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.block.BlockType;

final class SpongeBlockType extends RRegistryTypeHandle<BlockType> implements RBlockType {
    SpongeBlockType(@NotNull RKey key, @NotNull BlockType handle) {
        super(PlatformId.SPONGE, key, handle);
    }
}
