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

public final class SharedInventoryFeatureInstallerSupport {
    private static final int PLAYER_MENU_SLOT_COUNT = 36;

    private SharedInventoryFeatureInstallerSupport() {
    }

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
