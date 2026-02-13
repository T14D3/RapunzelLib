package de.t14d3.rapunzellib.inventory.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.inventory.InventoryFeatureInstaller;
import de.t14d3.rapunzellib.inventory.InventoryFeatureInstallerSupport;
import de.t14d3.rapunzellib.inventory.InventoryFeatureInstallerSupport.SlotInventoryAdapter;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;

import java.util.List;

public final class SpongeInventoryFeatureInstaller implements InventoryFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.SPONGE;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        ItemStackAdapter<ItemStack> itemAdapter = NbtFeatures.itemStackAdapter(ItemStack.class);
        InventoryFeatureInstallerSupport.registerInventories(
            context,
            PlatformId.SPONGE,
            List.of(InventoryFeatureInstallerSupport.slotInventoryFactory(
                PlatformId.SPONGE,
                SlotInventoryAdapter.<Inventory, ItemStack>builder(Inventory.class, itemAdapter)
                    .size(inventory -> inventory.slots().size())
                    .getItem((inventory, slot) -> slot(inventory, slot).peek())
                    .setItem((inventory, slot, item) -> slot(inventory, slot).set(item == null ? ItemStack.empty() : item))
                    .isEmptyItem(item -> item == null || item.isEmpty() || item.quantity() <= 0)
                    .emptyItem(ItemStack::empty)
                    .build()
            ))
        );
    }

    private static @NotNull Slot slot(@NotNull Inventory inventory, int index) {
        return inventory.slot(index)
            .orElseThrow(() -> new IllegalArgumentException("Missing slot " + index + " for inventory " + inventory));
    }
}
