package de.t14d3.rapunzellib.nbt.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.nbt.NbtFeatureInstaller;
import de.t14d3.rapunzellib.nbt.NbtFeatureInstallerSupport;
import de.t14d3.rapunzellib.nbt.NbtSerializer;
import org.jetbrains.annotations.NotNull;

public final class SpongeNbtFeatureInstaller implements NbtFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.SPONGE;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        SpongeItemStackAdapter adapter = new SpongeItemStackAdapter();
        NbtFeatureInstallerSupport.registerPrimaryItemStackAdapter(
            context,
            platformId(),
            org.spongepowered.api.item.inventory.ItemStack.class,
            SpongeItemStackAdapter.class,
            adapter
        );
        context.register(NbtSerializer.class, new SpongeNbtSerializer());
    }
}
