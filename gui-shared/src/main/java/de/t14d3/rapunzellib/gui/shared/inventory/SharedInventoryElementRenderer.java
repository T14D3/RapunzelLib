package de.t14d3.rapunzellib.gui.shared.inventory;

import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.core.GuiInventoryPresentation;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.Icon;
import de.t14d3.rapunzellib.gui.element.ItemElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.shared.SharedGuiComponents;
import de.t14d3.rapunzellib.nbt.item.ItemStackAdapter;
import net.kyori.adventure.text.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private @NotNull ItemStack renderItem(@NotNull ItemElement item) {
        ItemStack stack = itemAdapter.create(item.item());
        if (item.tooltip() != null) {
            List<Component> lore = new ArrayList<>();
            ItemLore existingLore = stack.get(DataComponents.LORE);
            if (existingLore != null) {
                existingLore.lines().stream().map(de.t14d3.rapunzellib.nbt.shared.SharedAdventureComponentCodec::toAdventure).forEach(lore::add);
            }
            lore.add(item.tooltip());
            applyLore(stack, lore);
        }
        return stack;
    }

    private @NotNull Item resolveItem(@Nullable Icon icon) {
        if (icon instanceof Icon.ItemIcon itemIcon) {
            // #if VERSION >= 1.21.11
            Identifier location = Identifier.tryParse(GuiInventoryPresentation.normalizeItemKey(itemIcon.itemId(), "minecraft:stone"));
            // #else
            ResourceLocation location = ResourceLocation.tryParse(GuiInventoryPresentation.normalizeItemKey(itemIcon.itemId(), "minecraft:stone"));
            // #endif
            if (location != null) {
                Item item = BuiltInRegistries.ITEM.getValue(location);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    return item;
                }
            }
        }
        return net.minecraft.world.item.Items.STONE;
    }

    private @NotNull Item resolveItem(@NotNull String itemKey) {
        // #if VERSION >= 1.21.11
        Identifier location = Identifier.tryParse(GuiInventoryPresentation.normalizeItemKey(itemKey, "minecraft:stone"));
        // #else
        ResourceLocation location = ResourceLocation.tryParse(GuiInventoryPresentation.normalizeItemKey(itemKey, "minecraft:stone"));
        // #endif
        if (location == null) {
            return net.minecraft.world.item.Items.STONE;
        }
        Item item = BuiltInRegistries.ITEM.getValue(location);
        return item != null ? item : net.minecraft.world.item.Items.STONE;
    }

    private @NotNull ItemStack renderPresentation(@NotNull GuiInventoryPresentation.Entry entry) {
        if (entry.empty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(resolveItem(entry.itemKey()));
        applyNameAndLore(stack, entry.label() != null ? entry.label() : Component.text(" "), entry.lore());
        if (entry.glow()) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        }
        return stack;
    }

    private @NotNull ItemStack namedStack(@NotNull Item item, @NotNull Component name, @NotNull List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        applyNameAndLore(stack, name, lore);
        return stack;
    }

    private void applyNameAndLore(@NotNull ItemStack stack, @NotNull Component name, @NotNull List<Component> lore) {
        stack.set(DataComponents.CUSTOM_NAME, SharedGuiComponents.toNative(name));
        applyLore(stack, lore);
    }

    private void applyLore(@NotNull ItemStack stack, @NotNull List<Component> lore) {
        if (lore.isEmpty()) {
            stack.remove(DataComponents.LORE);
            return;
        }
        List<net.minecraft.network.chat.Component> nativeLore = lore.stream().map(SharedGuiComponents::toNative).toList();
        stack.set(DataComponents.LORE, new ItemLore(nativeLore));
    }
}
