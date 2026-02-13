package de.t14d3.rapunzellib.inventory.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.inventory.InventoryFeatureInstaller;
import de.t14d3.rapunzellib.inventory.shared.SharedInventoryFeatureInstallerSupport;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeInventoryFeatureInstaller implements InventoryFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        ItemStackAdapter<ItemStack> itemAdapter = NbtFeatures.itemStackAdapter(ItemStack.class);
        SharedInventoryFeatureInstallerSupport.registerInventories(context, platformId(), itemAdapter);
    }
}
