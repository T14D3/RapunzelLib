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

/**
 * Renders GUI elements and dropdown options into native Minecraft {@link ItemStack}s.
 * <p>
 * Delegates item creation to an {@link ItemStackAdapter} and handles
 * presentation logic for various element types.
 */
public final class SharedInventoryElementRenderer {
    private final ItemStackAdapter<ItemStack> itemAdapter;

    /**
     * Creates a renderer backed by the given item stack adapter.
     *
     * @param itemAdapter the item stack adapter
     */
    public SharedInventoryElementRenderer(@NotNull ItemStackAdapter<ItemStack> itemAdapter) {
        this.itemAdapter = itemAdapter;
    }

    /**
     * Renders a GUI element into an ItemStack.
     *
     * @param element the GUI element
     * @param context the render context
     * @return the rendered ItemStack
     */
    public @NotNull ItemStack render(@NotNull GuiElement element, @NotNull RenderContext context) {
        if (element instanceof ItemElement item) {
            return renderItem(item);
        }
        return renderPresentation(GuiInventoryPresentation.present(element, context));
    }

    /**
     * Renders a dropdown option as an ItemStack.
     *
     * @param option   the dropdown option
     * @param selected whether the option is selected
     * @return the rendered ItemStack
     */
    public @NotNull ItemStack renderDropdownOption(
        @NotNull Option option,
        boolean selected
    ) {
        return renderPresentation(GuiInventoryPresentation.presentDropdownOption(option, selected));
    }

    /**
     * Renders an ItemElement into an ItemStack.
     *
     * @param element the item element
     * @return the rendered ItemStack
     */
    private @NotNull ItemStack renderItem(@NotNull ItemElement element) {
        RItem item = element.item();
        if (element.tooltip() != null) {
            List<Component> lore = new ArrayList<>(item.lore());
            lore.add(element.tooltip());
            item = item.withLore(lore);
        }
        return itemAdapter.create(item);
    }

    /**
     * Renders a presentation entry into an ItemStack.
     *
     * @param entry the presentation entry
     * @return the rendered ItemStack, or {@link ItemStack#EMPTY} if empty
     */
    private @NotNull ItemStack renderPresentation(@NotNull GuiInventoryPresentation.Entry entry) {
        if (entry.empty()) {
            return ItemStack.EMPTY;
        }
        return itemAdapter.create(entry.item());
    }
}
