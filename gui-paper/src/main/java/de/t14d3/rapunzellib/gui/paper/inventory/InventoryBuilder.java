package de.t14d3.rapunzellib.gui.paper.inventory;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.core.GuiInventoryPresentation;
import de.t14d3.rapunzellib.gui.core.GuiSlotPlan;
import de.t14d3.rapunzellib.gui.element.*;
import de.t14d3.rapunzellib.nbt.NbtFeatures;
import de.t14d3.rapunzellib.nbt.item.RItem;
import de.t14d3.rapunzellib.nbt.paper.PaperItemStackAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class InventoryBuilder {
    private static final ItemStack EMPTY_SLOT_FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

    static {
        var meta = EMPTY_SLOT_FILLER.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            EMPTY_SLOT_FILLER.setItemMeta(meta);
        }
    }

    private InventoryBuilder() {
    }

    public static void populateInventory(@NotNull Gui gui, @NotNull RenderContext context, @NotNull Inventory inventory) {
        int size = inventory.getSize();
        GuiSlotPlan slotPlan = GuiSlotPlan.resolve(gui, 6);

        for (Map.Entry<Integer, GuiElement> entry : slotPlan.slots().entrySet()) {
            int slot = entry.getKey();
            GuiElement element = entry.getValue();
            if (slot >= 0 && slot < size) {
                ItemStack stack = renderElement(element, context);
                if (stack != null) {
                    inventory.setItem(slot, stack);
                    context.registerElement(slot, element);
                }
            }
        }

        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, EMPTY_SLOT_FILLER);
            }
        }
    }

    public static @NotNull ItemStack renderElement(@NotNull GuiElement element, @NotNull RenderContext context) {
        if (element instanceof ItemElement item) {
            return convertItem(item);
        }
        return createItem(GuiInventoryPresentation.present(element, context));
    }

    public static @NotNull ItemStack renderDropdownOption(@NotNull Option option, boolean selected) {
        return createItem(GuiInventoryPresentation.presentDropdownOption(option, selected));
    }

    @NotNull
    private static ItemStack convertItem(@NotNull ItemElement item) {
        RItem rendered = item.item();
        if (item.tooltip() != null) {
            List<Component> lore = new ArrayList<>(rendered.lore());
            lore.add(item.tooltip());
            rendered = rendered.withLore(lore);
        }
        return adapter().create(rendered);
    }

    private static @NotNull ItemStack createItem(@NotNull GuiInventoryPresentation.Entry entry) {
        if (entry.empty()) {
            return new ItemStack(Material.AIR);
        }
        return adapter().create(entry.item());
    }

    private static @NotNull PaperItemStackAdapter adapter() {
        return (PaperItemStackAdapter) NbtFeatures.itemStackAdapter(ItemStack.class);
    }
}
