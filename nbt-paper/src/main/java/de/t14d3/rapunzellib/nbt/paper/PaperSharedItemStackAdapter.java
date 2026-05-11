package de.t14d3.rapunzellib.nbt.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.nbt.item.NativeRItem;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.nbt.shared.AbstractSharedItemStackAdapter;
import org.jetbrains.annotations.NotNull;

public final class PaperSharedItemStackAdapter extends AbstractSharedItemStackAdapter {
    public PaperSharedItemStackAdapter() {
        super(PlatformId.PAPER);
    }

    public @NotNull NativeRItem<net.minecraft.world.item.ItemStack> snapshotSharedLive(@NotNull net.minecraft.world.item.ItemStack nativeItem) {
        return createLive(nativeItem);
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
