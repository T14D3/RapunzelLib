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
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
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
        // Full combined menu view (top container + player inventory section).
        // Slot ids index the full menu slot list, matching Bukkit's
        // InventoryClickEvent#getRawSlot(); the native menu's slot count and
        // item lookup are reached through CraftInventoryView#getHandle() so the
        // wrap covers the FULL menu even on the player's own inventory screen
        // (where the Bukkit top+bottom slot count falls short of the menu).
        // The player inventory section starts exactly at the top inventory's
        // size (Bukkit's isInTop boundary) - this also covers the player's own
        // CRAFTING/CREATIVE views, where size() - 36 would not be the start
        // (armor and the offhand belong to the player section there).
        wrapperFactories.add(InventoryFeatureInstallerSupport.slotInventoryFactory(
            PlatformId.PAPER,
            SlotInventoryAdapter.<CraftInventoryView, ItemStack>builder(CraftInventoryView.class, bukkitItemAdapter)
                .size(view -> view.getHandle().slots.size())
                .playerInventoryStart(view -> view.getTopInventory().getSize())
                .getItem(CraftInventoryView::getItem)
                .setItem(CraftInventoryView::setItem)
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
