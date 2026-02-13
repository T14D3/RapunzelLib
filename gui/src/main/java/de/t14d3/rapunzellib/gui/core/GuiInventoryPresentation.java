package de.t14d3.rapunzellib.gui.core;

import de.t14d3.rapunzellib.gui.RenderContext;
import de.t14d3.rapunzellib.gui.element.ButtonElement;
import de.t14d3.rapunzellib.gui.element.DividerElement;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.Icon;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.element.PaginationElement;
import de.t14d3.rapunzellib.gui.element.SliderElement;
import de.t14d3.rapunzellib.gui.element.SpacerElement;
import de.t14d3.rapunzellib.gui.element.TextElement;
import de.t14d3.rapunzellib.gui.element.ToggleElement;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GuiInventoryPresentation {
    private static final String DEFAULT_BUTTON_ITEM = "minecraft:stone";

    private GuiInventoryPresentation() {
    }

    public static @NotNull Entry present(@NotNull GuiElement element, @NotNull RenderContext context) {
        return switch (element) {
            case ButtonElement button -> button(button);
            case TextElement text -> new Entry("minecraft:paper", text.text(), List.of(), false, false);
            case InputElement input -> input(input, context);
            case ToggleElement toggle -> toggle(toggle, context);
            case SliderElement slider -> slider(slider, context);
            case DropdownElement dropdown -> dropdown(dropdown, context);
            case PaginationElement pagination -> pagination(pagination);
            case DividerElement ignored -> new Entry("minecraft:gray_stained_glass_pane", Component.text(" "), List.of(), false, false);
            case SpacerElement ignored -> Entry.blank();
            default -> new Entry(DEFAULT_BUTTON_ITEM, Component.text("Unsupported"), List.of(), false, false);
        };
    }

    public static @NotNull Entry presentDropdownOption(@NotNull Option option, boolean selected) {
        List<Component> lore = selected ? List.of(Component.text("Selected")) : List.of();
        return new Entry(
            selected ? "minecraft:lime_stained_glass_pane" : "minecraft:gray_stained_glass_pane",
            selected ? Component.text("> ").append(option.display()) : option.display(),
            lore,
            false,
            false
        );
    }

    public static @NotNull Component labelOrKey(@NotNull String key, @Nullable Component label) {
        return label != null ? label : Component.text(key);
    }

    public static @NotNull String iconItemKey(@Nullable Icon icon, @NotNull String fallback) {
        if (icon instanceof Icon.ItemIcon itemIcon) {
            return normalizeItemKey(itemIcon.itemId(), fallback);
        }
        return fallback;
    }

    public static @NotNull String normalizeItemKey(@Nullable String itemKey, @NotNull String fallback) {
        if (itemKey == null || itemKey.isBlank()) {
            return fallback;
        }
        return itemKey.contains(":") ? itemKey.toLowerCase() : "minecraft:" + itemKey.toLowerCase();
    }

    private static @NotNull Entry button(@NotNull ButtonElement button) {
        List<Component> lore = button.tooltip() != null ? List.of(button.tooltip()) : List.of();
        return new Entry(iconItemKey(button.icon(), DEFAULT_BUTTON_ITEM), button.label(), lore, !button.enabled(), false);
    }

    private static @NotNull Entry input(@NotNull InputElement input, @NotNull RenderContext context) {
        List<Component> lore = new ArrayList<>();
        String currentValue = GuiElementStates.inputValue(input, context.state());
        if (!currentValue.isEmpty()) {
            lore.add(Component.text("Current: " + currentValue));
        } else if (input.defaultValue() != null && !input.defaultValue().isEmpty()) {
            lore.add(Component.text("Default: " + input.defaultValue()));
        }
        if (input.placeholder() != null && !input.placeholder().isEmpty()) {
            lore.add(Component.text("Hint: " + input.placeholder()));
        }
        lore.add(Component.text("Click to edit"));
        return new Entry("minecraft:writable_book", labelOrKey(input.key(), input.label()), lore, false, false);
    }

    private static @NotNull Entry toggle(@NotNull ToggleElement toggle, @NotNull RenderContext context) {
        boolean value = GuiElementStates.toggleValue(toggle, context.state());
        return new Entry(
            value ? "minecraft:lime_wool" : "minecraft:red_wool",
            labelOrKey(toggle.key(), toggle.label()),
            List.of(Component.text(value ? "Enabled" : "Disabled"), Component.text("Click to toggle")),
            false,
            false
        );
    }

    private static @NotNull Entry slider(@NotNull SliderElement slider, @NotNull RenderContext context) {
        float value = GuiElementStates.sliderValue(slider, context.state());
        int percent = GuiElementStates.sliderPercent(slider, context.state());
        int filledBars = Math.round((percent / 100.0f) * 10.0f);
        return new Entry(
            "minecraft:repeater",
            labelOrKey(slider.key(), slider.label()),
            List.of(
                Component.text("[" + GuiElementStates.progressBar(filledBars, 10, '#', '-') + "]"),
                Component.text(String.format("Value: %.1f", value)),
                Component.text(String.format("Range: %.1f - %.1f", slider.min(), slider.max())),
                Component.text(String.format("Step: %.1f", slider.step())),
                Component.text("Left-click: +" + slider.step()),
                Component.text("Right-click: -" + slider.step())
            ),
            false,
            false
        );
    }

    private static @NotNull Entry dropdown(@NotNull DropdownElement dropdown, @NotNull RenderContext context) {
        GuiElementStates.DropdownState state = GuiElementStates.dropdown(dropdown, context.state());
        List<Component> lore = new ArrayList<>();
        if (state.selectedOption() != null) {
            lore.add(Component.text("Selected: ").append(state.selectedOption().display()));
        } else if (dropdown.defaultValue() != null) {
            lore.add(Component.text("Default: ").append(dropdown.defaultValue().display()));
        }
        for (Option option : dropdown.options()) {
            lore.add(Component.text(option.id().equals(state.selectedId()) ? "> " : "  ").append(option.display()));
        }
        lore.add(Component.text("Click to select"));
        return new Entry("minecraft:hopper", labelOrKey(dropdown.key(), dropdown.label()), lore, false, false);
    }

    private static @NotNull Entry pagination(@NotNull PaginationElement pagination) {
        return new Entry(
            "minecraft:book",
            Component.text("Pagination"),
            List.of(Component.text("Page " + (pagination.currentPage() + 1) + " of " + pagination.totalPages())),
            false,
            false
        );
    }

    public record Entry(
        @NotNull String itemKey,
        @Nullable Component label,
        @NotNull List<Component> lore,
        boolean glow,
        boolean empty
    ) {
        public Entry {
            lore = List.copyOf(lore);
        }

        public static @NotNull Entry blank() {
            return new Entry("minecraft:air", Component.text(" "), List.of(), false, true);
        }
    }
}
