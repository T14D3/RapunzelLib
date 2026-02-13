package de.t14d3.rapunzellib.inventory.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.inventory.InventoryFeatureInstaller;
import de.t14d3.rapunzellib.inventory.InventoryFeatureInstallerSupport;
import de.t14d3.rapunzellib.inventory.InventoryFeatureInstallerSupport.SlotInventoryAdapter;
import de.t14d3.rapunzellib.inventory.InventoryWrapperFactory;
import de.t14d3.rapunzellib.inventory.shared.SharedInventoryFeatureInstallerSupport;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class PaperInventoryFeatureInstaller implements InventoryFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        ItemStackAdapter<ItemStack> bukkitItemAdapter = NbtFeatures.itemStackAdapter(ItemStack.class);
        ItemStackAdapter<net.minecraft.world.item.ItemStack> nativeItemAdapter =
            NbtFeatures.itemStackAdapter(net.minecraft.world.item.ItemStack.class);

        List<InventoryWrapperFactory<?>> wrapperFactories = new ArrayList<>();
        wrapperFactories.add(InventoryFeatureInstallerSupport.slotInventoryFactory(
            PlatformId.PAPER,
            SlotInventoryAdapter.<Inventory, ItemStack>builder(Inventory.class, bukkitItemAdapter)
                .size(Inventory::getSize)
                .getItem(Inventory::getItem)
                .setItem(Inventory::setItem)
                .clear(Inventory::clear)
                .isEmptyItem(item -> item == null || item.getType().isAir() || item.getAmount() <= 0)
                .emptyItem((ItemStack) null)
                .build()
        ));
        wrapperFactories.addAll(SharedInventoryFeatureInstallerSupport.wrapperFactories(PlatformId.PAPER, nativeItemAdapter));

        InventoryFeatureInstallerSupport.registerInventories(
            context,
            PlatformId.PAPER,
            wrapperFactories
        );
    }
}
