package de.t14d3.rapunzellib.nbt.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.nbt.shared.AbstractSharedItemStackAdapter;
import org.jetbrains.annotations.NotNull;

public final class PaperSharedItemStackAdapter extends AbstractSharedItemStackAdapter {
    public PaperSharedItemStackAdapter() {
        super(PlatformId.PAPER);
    }

    public @NotNull RItem snapshotShared(@NotNull net.minecraft.world.item.ItemStack nativeItem) {
        return toShared(nativeItem);
    }

    public @NotNull net.minecraft.world.item.ItemStack createShared(@NotNull RItem item) {
        return createNativeShared(item);
    }

    public @NotNull net.minecraft.world.item.ItemStack applyShared(
        @NotNull net.minecraft.world.item.ItemStack nativeItem,
        @NotNull RItem item
    ) {
        return updateNativeShared(nativeItem, item);
    }
}
