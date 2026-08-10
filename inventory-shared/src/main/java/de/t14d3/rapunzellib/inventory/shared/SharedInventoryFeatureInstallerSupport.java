package de.t14d3.rapunzellib.inventory.shared;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.inventory.InventoryFeatureInstallerSupport;
import de.t14d3.rapunzellib.inventory.InventoryFeatureInstallerSupport.SlotInventoryAdapter;
import de.t14d3.rapunzellib.inventory.InventoryWrapperFactory;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Shared support for registering inventory wrappers for {@link Container}
 * and {@link AbstractContainerMenu} types.
 * <p>
 * Provides factories that wrap native Minecraft inventories into Rapunzel's
 * {@link de.t14d3.rapunzellib.inventory.RInventory} abstraction.
 */
public final class SharedInventoryFeatureInstallerSupport {
    private SharedInventoryFeatureInstallerSupport() {
    }

    /**
     * Registers both {@link Container} and {@link AbstractContainerMenu}
     * inventory wrappers in the context.
     *
     * @param context    the Rapunzel context
     * @param platformId the platform identifier
     * @param itemAdapter the item stack adapter for slot item conversion
     */
    public static void registerInventories(
        @NotNull RapunzelContext context,
        @NotNull PlatformId platformId,
        @NotNull ItemStackAdapter<ItemStack> itemAdapter
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(itemAdapter, "itemAdapter");

        InventoryFeatureInstallerSupport.registerInventories(
            context,
            platformId,
            wrapperFactories(platformId, itemAdapter)
        );
    }

    /**
     * Creates the list of inventory wrapper factories for Container and AbstractContainerMenu.
     *
     * @param platformId  the platform identifier
     * @param itemAdapter the item stack adapter
     * @return the list of wrapper factories
     */
    public static @NotNull List<InventoryWrapperFactory<?>> wrapperFactories(
        @NotNull PlatformId platformId,
        @NotNull ItemStackAdapter<ItemStack> itemAdapter
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(itemAdapter, "itemAdapter");
        return List.of(containerFactory(platformId, itemAdapter), menuFactory(platformId, itemAdapter));
    }

    private static @NotNull InventoryWrapperFactory<Container> containerFactory(
        @NotNull PlatformId platformId,
        @NotNull ItemStackAdapter<ItemStack> itemAdapter
    ) {
        return InventoryFeatureInstallerSupport.slotInventoryFactory(
            platformId,
            SlotInventoryAdapter.<Container, ItemStack>builder(Container.class, itemAdapter)
                .size(Container::getContainerSize)
                .getItem(Container::getItem)
                .setItem((container, slot, item) -> {
                    container.setItem(slot, item == null ? ItemStack.EMPTY : item);
                    container.setChanged();
                })
                .clear(Container::clearContent)
                .isEmptyItem(item -> item == null || item.isEmpty())
                .emptyItem(ItemStack.EMPTY)
                .build()
        );
    }

    private static @NotNull InventoryWrapperFactory<AbstractContainerMenu> menuFactory(
        @NotNull PlatformId platformId,
        @NotNull ItemStackAdapter<ItemStack> itemAdapter
    ) {
        return InventoryFeatureInstallerSupport.slotInventoryFactory(
            platformId,
            SlotInventoryAdapter.<AbstractContainerMenu, ItemStack>builder(AbstractContainerMenu.class, itemAdapter)
                // The wrap covers the FULL combined menu: the top container plus
                // the player inventory section. Slot ids therefore index the full
                // menu slot list, matching Bukkit's InventoryClickEvent#getRawSlot()
                // (the raw slot id in the combined menu, 0..slots.size()-1).
                .size(menu -> menu.slots.size())
                // The player section spans the last 36 slots of every vanilla
                // container menu (27 main inventory + 9 hotbar slots), so the
                // start is menuSlots - 36. Exact for all container menus; the
                // player's own CRAFTING view (armor/offhand in the player
                // section) is covered exactly on platforms that wrap the view
                // (Paper), where the view adapter computes the top size.
                .playerInventoryStart(menu -> menu.slots.size() - 36)
                .getItem((menu, slot) -> menu.getSlot(slot).getItem())
                .setItem((menu, slot, item) -> {
                    menu.getSlot(slot).set(item == null ? ItemStack.EMPTY : item);
                    menu.broadcastChanges();
                })
                .isEmptyItem(item -> item == null || item.isEmpty())
                .emptyItem(ItemStack.EMPTY)
                .build()
        );
    }
}
