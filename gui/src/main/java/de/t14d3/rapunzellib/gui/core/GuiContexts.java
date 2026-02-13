package de.t14d3.rapunzellib.gui.core;

import de.t14d3.rapunzellib.gui.context.ClickContext;
import de.t14d3.rapunzellib.gui.context.ClickType;
import de.t14d3.rapunzellib.gui.context.DropdownContext;
import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.context.InputContext;
import de.t14d3.rapunzellib.gui.context.SliderContext;
import de.t14d3.rapunzellib.gui.context.ToggleContext;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GuiContexts {
    private GuiContexts() {
    }

    public static @NotNull ClickContext click(
        @NotNull RPlayer player,
        @NotNull GuiElement element,
        int slot,
        @NotNull ClickType clickType,
        @NotNull GuiState state
    ) {
        return new ClickContextImpl(player, element, slot, clickType, state);
    }

    public static @NotNull InputContext input(
        @NotNull RPlayer player,
        @NotNull String key,
        @NotNull String value
    ) {
        return new InputContextImpl(player, key, value);
    }

    public static @NotNull ToggleContext toggle(
        @NotNull RPlayer player,
        @NotNull String key,
        boolean value
    ) {
        return new ToggleContextImpl(player, key, value);
    }

    public static @NotNull SliderContext slider(
        @NotNull RPlayer player,
        @NotNull String key,
        float value
    ) {
        return new SliderContextImpl(player, key, value);
    }

    public static @NotNull DropdownContext dropdown(
        @NotNull RPlayer player,
        @NotNull String key,
        @NotNull String selectedId,
        @Nullable Option selectedOption
    ) {
        return new DropdownContextImpl(player, key, selectedId, selectedOption);
    }

    private record ClickContextImpl(
        @NotNull RPlayer player,
        @NotNull GuiElement element,
        int slot,
        @NotNull ClickType clickType,
        @NotNull GuiState state
    ) implements ClickContext {
    }

    private record InputContextImpl(
        @NotNull RPlayer player,
        @NotNull String key,
        @NotNull String value
    ) implements InputContext {
    }

    private record ToggleContextImpl(
        @NotNull RPlayer player,
        @NotNull String key,
        boolean value
    ) implements ToggleContext {
    }

    private record SliderContextImpl(
        @NotNull RPlayer player,
        @NotNull String key,
        float value
    ) implements SliderContext {
    }

    private record DropdownContextImpl(
        @NotNull RPlayer player,
        @NotNull String key,
        @NotNull String selectedId,
        @Nullable Option selectedOption
    ) implements DropdownContext {
    }
}
