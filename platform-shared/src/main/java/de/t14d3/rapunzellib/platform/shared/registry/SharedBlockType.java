package de.t14d3.rapunzellib.platform.shared.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.RRegistryTypeHandle;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

final class SharedBlockType extends RRegistryTypeHandle<Block> implements RBlockType {
    SharedBlockType(@NotNull PlatformId platformId, @NotNull RKey key, @NotNull Block handle) {
        super(platformId, key, handle);
    }
}
