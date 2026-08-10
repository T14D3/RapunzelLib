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
// #if VERSION >= 26.2
// # import org.bukkit.craftbukkit.inventory.CraftAbstractInventoryView;
// #endif
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
        // 26.2+: virtual views created via Bukkit.createInventory (shulker
        // editors, enderchest, invsee mirrors) are CraftContainer$1 instances
        // of CraftAbstractInventoryView - CraftContainer no longer extends
        // CraftInventoryView there, so the factory above never matches them
        // and every click in such a view dies in require(). Register a
        // fallback factory for the abstract view type AFTER the concrete one:
        // real menu views still match CraftInventoryView first; the fallback
        // (full menu = top + 36 player slots, raw-slot item lookup via the
        // view) only catches the virtual-view class. The top inventory of a
        // virtual view is always the created inventory, so top + 36 is exact.
        // #if VERSION >= 26.2
        // # wrapperFactories.add(InventoryFeatureInstallerSupport.slotInventoryFactory(
        // #     PlatformId.PAPER,
        // #     SlotInventoryAdapter.<CraftAbstractInventoryView, ItemStack>builder(CraftAbstractInventoryView.class, bukkitItemAdapter)
        // #         .size(view -> view.getTopInventory().getSize() + 36)
        // #         .playerInventoryStart(view -> view.getTopInventory().getSize())
        // #         .getItem(CraftAbstractInventoryView::getItem)
        // #         .setItem(CraftAbstractInventoryView::setItem)
        // #         .isEmptyItem(item -> item == null || item.getType().isAir() || item.getAmount() <= 0)
        // #         .emptyItem((ItemStack) null)
        // #         .build()
        // # ));
        // #endif
        wrapperFactories.addAll(SharedInventoryFeatureInstallerSupport.wrapperFactories(PlatformId.PAPER, nativeItemAdapter));

        InventoryFeatureInstallerSupport.registerInventories(
            context,
            PlatformId.PAPER,
            wrapperFactories
        );
    }
}
