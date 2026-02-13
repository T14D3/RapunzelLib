package de.t14d3.rapunzellib.gui.dialog;

import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.core.GuiElementStates;
import de.t14d3.rapunzellib.gui.core.GuiInteractionEngine;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.element.SliderElement;
import de.t14d3.rapunzellib.gui.element.ToggleElement;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface GuiDialogField permits GuiDialogField.InputField,
    GuiDialogField.ToggleField,
    GuiDialogField.SliderField,
    GuiDialogField.DropdownField {

    @NotNull String key();

    @NotNull GuiElement element();

    @Nullable Component label();

    default @NotNull Component labelOrKey() {
        return label() != null ? label() : Component.text(key());
    }

    @NotNull GuiDialogFieldValue currentValue(@NotNull GuiState state);

    @NotNull GuiDialogFieldValue submittedValue(@NotNull GuiValue value);

    void submit(@Nullable GuiDialogFieldValue value, @NotNull RPlayer player, @NotNull GuiState state);

    static @Nullable GuiDialogField of(@NotNull GuiElement element) {
        return switch (element) {
            case InputElement input -> new InputField(input);
            case ToggleElement toggle -> new ToggleField(toggle);
            case SliderElement slider -> new SliderField(slider);
            case DropdownElement dropdown -> new DropdownField(dropdown);
            default -> null;
        };
    }

    record InputField(@NotNull InputElement element) implements GuiDialogField {
        @Override
        public @NotNull String key() {
            return element.key();
        }

        @Override
        public @Nullable Component label() {
            return element.label();
        }

        @Override
        public @NotNull GuiDialogFieldValue.TextValue currentValue(@NotNull GuiState state) {
            return new GuiDialogFieldValue.TextValue(GuiElementStates.inputValue(element, state));
        }

        @Override
        public @NotNull GuiDialogFieldValue.TextValue submittedValue(@NotNull GuiValue value) {
            return new GuiDialogFieldValue.TextValue(value.stringValue(""));
        }

        @Override
        public void submit(@Nullable GuiDialogFieldValue value, @NotNull RPlayer player, @NotNull GuiState state) {
            GuiDialogFieldValue.TextValue typedValue = value instanceof GuiDialogFieldValue.TextValue textValue
                ? textValue
                : currentValue(state);
            GuiInteractionEngine.submitInput(element, state, player, typedValue);
        }
    }

    record ToggleField(@NotNull ToggleElement element) implements GuiDialogField {
        @Override
        public @NotNull String key() {
            return element.key();
        }

        @Override
        public @Nullable Component label() {
            return element.label();
        }

        @Override
        public @NotNull GuiDialogFieldValue.ToggleValue currentValue(@NotNull GuiState state) {
            return new GuiDialogFieldValue.ToggleValue(GuiElementStates.toggleValue(element, state));
        }

        @Override
        public @NotNull GuiDialogFieldValue.ToggleValue submittedValue(@NotNull GuiValue value) {
            return new GuiDialogFieldValue.ToggleValue(value.booleanValue(element.defaultValue()));
        }

        @Override
        public void submit(@Nullable GuiDialogFieldValue value, @NotNull RPlayer player, @NotNull GuiState state) {
            GuiDialogFieldValue.ToggleValue typedValue = value instanceof GuiDialogFieldValue.ToggleValue toggleValue
                ? toggleValue
                : currentValue(state);
            GuiInteractionEngine.assignToggle(element, state, player, typedValue);
        }
    }

    record SliderField(@NotNull SliderElement element) implements GuiDialogField {
        @Override
        public @NotNull String key() {
            return element.key();
        }

        @Override
        public @Nullable Component label() {
            return element.label();
        }

        @Override
        public @NotNull GuiDialogFieldValue.SliderValue currentValue(@NotNull GuiState state) {
            return new GuiDialogFieldValue.SliderValue(GuiElementStates.sliderValue(element, state));
        }

        @Override
        public @NotNull GuiDialogFieldValue.SliderValue submittedValue(@NotNull GuiValue value) {
            return new GuiDialogFieldValue.SliderValue(value.floatValue(element.defaultValue()));
        }

        @Override
        public void submit(@Nullable GuiDialogFieldValue value, @NotNull RPlayer player, @NotNull GuiState state) {
            GuiDialogFieldValue.SliderValue typedValue = value instanceof GuiDialogFieldValue.SliderValue sliderValue
                ? sliderValue
                : currentValue(state);
            GuiInteractionEngine.assignSlider(element, state, player, typedValue);
        }
    }

    record DropdownField(@NotNull DropdownElement element) implements GuiDialogField {
        @Override
        public @NotNull String key() {
            return element.key();
        }

        @Override
        public @Nullable Component label() {
            return element.label();
        }

        @Override
        public @NotNull GuiDialogFieldValue.DropdownValue currentValue(@NotNull GuiState state) {
            GuiElementStates.DropdownState dropdownState = GuiElementStates.dropdown(element, state);
            return new GuiDialogFieldValue.DropdownValue(dropdownState.selectedId(), dropdownState.selectedOption());
        }

        @Override
        public @NotNull GuiDialogFieldValue.DropdownValue submittedValue(@NotNull GuiValue value) {
            String selectedId = value.stringValue("");
            Option selectedOption = GuiInteractionEngine.resolveOption(element, selectedId);
            return new GuiDialogFieldValue.DropdownValue(selectedId, selectedOption);
        }

        @Override
        public void submit(@Nullable GuiDialogFieldValue value, @NotNull RPlayer player, @NotNull GuiState state) {
            GuiDialogFieldValue.DropdownValue typedValue = value instanceof GuiDialogFieldValue.DropdownValue dropdownValue
                ? dropdownValue
                : currentValue(state);
            GuiInteractionEngine.selectDropdown(element, state, player, typedValue);
        }
    }
}
