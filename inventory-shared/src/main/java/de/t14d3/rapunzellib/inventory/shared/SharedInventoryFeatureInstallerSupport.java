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
    private static final int PLAYER_MENU_SLOT_COUNT = 36;

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

    /**
     * Creates a wrapper factory for {@link Container}.
     *
     * @param platformId  the platform identifier
     * @param itemAdapter the item stack adapter
     * @return the container wrapper factory
     */
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

    /**
     * Creates a wrapper factory for {@link AbstractContainerMenu}.
     *
     * @param platformId  the platform identifier
     * @param itemAdapter the item stack adapter
     * @return the menu wrapper factory
     */
    private static @NotNull InventoryWrapperFactory<AbstractContainerMenu> menuFactory(
        @NotNull PlatformId platformId,
        @NotNull ItemStackAdapter<ItemStack> itemAdapter
    ) {
        return InventoryFeatureInstallerSupport.slotInventoryFactory(
            platformId,
            SlotInventoryAdapter.<AbstractContainerMenu, ItemStack>builder(AbstractContainerMenu.class, itemAdapter)
                .size(menu -> Math.max(0, menu.slots.size() - PLAYER_MENU_SLOT_COUNT))
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
