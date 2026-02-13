package de.t14d3.rapunzellib.gui.core;

import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.dialog.GuiDialogFieldValue;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.element.SliderElement;
import de.t14d3.rapunzellib.gui.element.ToggleElement;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GuiInteractionEngine {
    private GuiInteractionEngine() {
    }

    public static boolean toggle(
        @NotNull ToggleElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player
    ) {
        return assignToggle(element, state, player, !state.get(element.key(), Boolean.class, element.defaultValue()));
    }

    public static boolean assignToggle(
        @NotNull ToggleElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @NotNull GuiDialogFieldValue.ToggleValue submittedValue
    ) {
        return assignToggle(element, state, player, submittedValue.stateValue());
    }

    public static boolean assignToggle(
        @NotNull ToggleElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @Nullable GuiValue submittedValue
    ) {
        boolean currentValue = state.get(element.key(), Boolean.class, element.defaultValue());
        boolean newValue = submittedValue != null ? submittedValue.booleanValue(currentValue) : currentValue;
        state.setBoolean(element.key(), newValue);
        if (element.onChange() != null) {
            element.onChange().accept(GuiContexts.toggle(player, element.key(), newValue));
        }
        return newValue;
    }

    public static boolean assignToggle(
        @NotNull ToggleElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        boolean submittedValue
    ) {
        return assignToggle(element, state, player, GuiValue.of(submittedValue));
    }

    public static float slide(
        @NotNull SliderElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        float delta
    ) {
        float currentValue = state.get(element.key(), Float.class, element.defaultValue());
        return assignSlider(element, state, player, Float.valueOf(currentValue + delta));
    }

    public static float assignSlider(
        @NotNull SliderElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @NotNull GuiDialogFieldValue.SliderValue submittedValue
    ) {
        return assignSlider(element, state, player, submittedValue.stateValue());
    }

    public static float assignSlider(
        @NotNull SliderElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @Nullable GuiValue submittedValue
    ) {
        float currentValue = state.get(element.key(), Float.class, element.defaultValue());
        float newValue = submittedValue != null ? submittedValue.floatValue(currentValue) : currentValue;
        newValue = Math.max(element.min(), Math.min(element.max(), newValue));
        state.setNumber(element.key(), newValue);
        if (element.onChange() != null) {
            element.onChange().accept(GuiContexts.slider(player, element.key(), newValue));
        }
        return newValue;
    }

    public static float assignSlider(
        @NotNull SliderElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        float submittedValue
    ) {
        return assignSlider(element, state, player, GuiValue.of(submittedValue));
    }

    public static @NotNull String submitInput(
        @NotNull InputElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @NotNull GuiDialogFieldValue.TextValue submittedValue
    ) {
        return submitInput(element, state, player, submittedValue.stateValue());
    }

    public static @NotNull String submitInput(
        @NotNull InputElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @Nullable GuiValue submittedValue
    ) {
        String value = submittedValue != null ? submittedValue.stringValue("") : "";
        if (element.maxLength() > 0 && value.length() > element.maxLength()) {
            value = value.substring(0, element.maxLength());
        }
        state.setString(element.key(), value);
        if (element.onChange() != null) {
            element.onChange().accept(GuiContexts.input(player, element.key(), value));
        }
        return value;
    }

    public static @NotNull String submitInput(
        @NotNull InputElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @Nullable String submittedValue
    ) {
        return submitInput(element, state, player, submittedValue != null ? GuiValue.of(submittedValue) : null);
    }

    public static @NotNull DropdownSelection selectDropdown(
        @NotNull DropdownElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @NotNull GuiDialogFieldValue.DropdownValue submittedValue
    ) {
        return selectDropdown(element, state, player, submittedValue.selectedId(), submittedValue.selectedOption());
    }

    public static @NotNull DropdownSelection selectDropdown(
        @NotNull DropdownElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @NotNull Option selectedOption
    ) {
        return selectDropdown(element, state, player, selectedOption.id(), selectedOption);
    }

    public static @NotNull DropdownSelection selectDropdown(
        @NotNull DropdownElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @Nullable GuiValue submittedValue
    ) {
        String selectedId = submittedValue != null
            ? submittedValue.stringValue("")
            : element.defaultValue() != null ? element.defaultValue().id() : "";
        Option selectedOption = resolveOption(element, selectedId);
        if (selectedOption == null && submittedValue == null) {
            selectedOption = element.defaultValue();
        }
        return selectDropdown(element, state, player, selectedId, selectedOption);
    }

    public static @NotNull DropdownSelection selectDropdown(
        @NotNull DropdownElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @Nullable String submittedValue
    ) {
        return selectDropdown(element, state, player, submittedValue != null ? GuiValue.of(submittedValue) : null);
    }

    private static @NotNull DropdownSelection selectDropdown(
        @NotNull DropdownElement element,
        @NotNull GuiState state,
        @NotNull RPlayer player,
        @NotNull String selectedId,
        @Nullable Option selectedOption
    ) {
        state.setString(element.key(), selectedId);
        if (element.onChange() != null) {
            element.onChange().accept(GuiContexts.dropdown(player, element.key(), selectedId, selectedOption));
        }
        return new DropdownSelection(selectedId, selectedOption);
    }

    public static @Nullable Option resolveOption(@NotNull DropdownElement element, @Nullable String selectedId) {
        if (selectedId == null) {
            return element.defaultValue();
        }
        for (Option option : element.options()) {
            if (option.id().equals(selectedId)) {
                return option;
            }
        }
        return element.defaultValue();
    }

    public record DropdownSelection(@NotNull String selectedId, @Nullable Option selectedOption) {
    }
}
