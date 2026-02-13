package de.t14d3.rapunzellib.gui.sponge.inventory;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.core.GuiInventoryPresentation;
import de.t14d3.rapunzellib.gui.core.GuiSlotPlan;
import de.t14d3.rapunzellib.gui.element.*;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

import java.util.Map;

final class SpongeInventoryBuilder {

    SpongeInventoryBuilder(InventoryRenderer renderer) {
    }

    ViewableInventory build(@NotNull Gui gui, @NotNull GuiSlotPlan slotPlan, @NotNull RenderContext context) {
        int slotCount = slotPlan.size();
        int rows = Math.max(1, Math.min(6, (slotCount + 8) / 9));
        Map<Integer, GuiElement> elements = slotPlan.slots();

        for (int i = 0; i < slotCount; i++) {
            GuiElement element = elements.get(i);
            if (element != null) {
                context.registerElement(i, element);
            }
        }

        ViewableInventory inventory = ViewableInventory.builder()
            .type(containerType(rows))
            .fillDummy()
            .completeStructure()
            .build();

        int i = 0;
        for (Inventory inv : inventory.slots()) {
            if (inv instanceof Slot slot) {
                GuiElement element = elements.get(i);
                ItemStack item = element != null ? renderElement(element, context) : createEmptySlot();
                slot.set(item);
            }
            i++;
        }

        return inventory;
    }

    static @NotNull ItemStack renderElement(@NotNull GuiElement element, @NotNull RenderContext context) {
        if (element instanceof ItemElement itemElement) {
            return NbtFeatures.itemStackAdapter(ItemStack.class).create(itemElement.item());
        }
        return createItem(GuiInventoryPresentation.present(element, context));
    }

    static @NotNull ItemStack renderDropdownOption(@NotNull Option option, boolean selected) {
        return createItem(GuiInventoryPresentation.presentDropdownOption(option, selected));
    }

    private static @NotNull ItemStack createItem(@NotNull GuiInventoryPresentation.Entry entry) {
        if (entry.empty()) {
            return createEmptySlot();
        }

        ItemStack.Builder builder = ItemStack.builder()
            .itemType(resolveItemType(entry.itemKey()))
            .quantity(1);

        if (entry.label() != null) {
            builder.add(Keys.CUSTOM_NAME, entry.label());
        }
        if (!entry.lore().isEmpty()) {
            builder.add(Keys.LORE, entry.lore());
        }
        return builder.build();
    }

    private static @NotNull ItemStack createEmptySlot() {
        return ItemStack.empty();
    }

    private static @NotNull org.spongepowered.api.item.ItemType resolveItemType(@NotNull String itemId) {
        return Sponge.server().registry(org.spongepowered.api.registry.RegistryTypes.ITEM_TYPE)
            .findValue(org.spongepowered.api.ResourceKey.resolve(itemId))
            .orElse(ItemTypes.STONE.get());
    }

    private static @NotNull org.spongepowered.api.item.inventory.ContainerType containerType(int rows) {
        return switch (rows) {
            case 1 -> ContainerTypes.GENERIC_9X1.get();
            case 2 -> ContainerTypes.GENERIC_9X2.get();
            case 3 -> ContainerTypes.GENERIC_9X3.get();
            case 4 -> ContainerTypes.GENERIC_9X4.get();
            case 5 -> ContainerTypes.GENERIC_9X5.get();
            default -> ContainerTypes.GENERIC_9X6.get();
        };
    }
}
