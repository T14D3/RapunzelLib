package de.t14d3.rapunzellib.nbt.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.nbt.NbtFeatureInstaller;
import de.t14d3.rapunzellib.nbt.NbtFeatureInstallerSupport;
import de.t14d3.rapunzellib.nbt.shared.SharedNbtFeatureInstallerSupport;
import org.jetbrains.annotations.NotNull;

public final class PaperNbtFeatureInstaller implements NbtFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        NbtFeatureInstallerSupport.registerPrimaryItemStackAdapter(
            context,
            platformId(),
            org.bukkit.inventory.ItemStack.class,
            PaperItemStackAdapter.class,
            new PaperItemStackAdapter()
        );
        NbtFeatureInstallerSupport.registerItemStackAdapter(
            context,
            platformId(),
            net.minecraft.world.item.ItemStack.class,
            PaperSharedItemStackAdapter.class,
            new PaperSharedItemStackAdapter()
        );
        SharedNbtFeatureInstallerSupport.registerSerializers(context);
    }
}
