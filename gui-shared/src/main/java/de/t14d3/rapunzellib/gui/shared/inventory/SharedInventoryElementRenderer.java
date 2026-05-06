package de.t14d3.rapunzellib.gui.shared.inventory;

import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.core.GuiInventoryPresentation;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.ItemElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import de.t14d3.rapunzellib.nbt.item.RItem;
import net.kyori.adventure.text.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SharedInventoryElementRenderer {
    private final ItemStackAdapter<ItemStack> itemAdapter;

    public SharedInventoryElementRenderer(@NotNull ItemStackAdapter<ItemStack> itemAdapter) {
        this.itemAdapter = itemAdapter;
    }

    public @NotNull ItemStack render(@NotNull GuiElement element, @NotNull RenderContext context) {
        if (element instanceof ItemElement item) {
            return renderItem(item);
        }
        return renderPresentation(GuiInventoryPresentation.present(element, context));
    }

    public @NotNull ItemStack renderDropdownOption(
        @NotNull Option option,
        boolean selected
    ) {
        return renderPresentation(GuiInventoryPresentation.presentDropdownOption(option, selected));
    }

    private @NotNull ItemStack renderItem(@NotNull ItemElement element) {
        RItem item = element.item();
        if (element.tooltip() != null) {
            List<Component> lore = new ArrayList<>(item.lore());
            lore.add(element.tooltip());
            item = item.withLore(lore);
        }
        return itemAdapter.create(item);
    }

    private @NotNull ItemStack renderPresentation(@NotNull GuiInventoryPresentation.Entry entry) {
        if (entry.empty()) {
            return ItemStack.EMPTY;
        }
        return itemAdapter.create(entry.item());
    }
}
