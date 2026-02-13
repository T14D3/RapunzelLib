package de.t14d3.rapunzellib.gui.dialog;

import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface GuiDialogFieldValue permits GuiDialogFieldValue.TextValue,
    GuiDialogFieldValue.ToggleValue,
    GuiDialogFieldValue.SliderValue,
    GuiDialogFieldValue.DropdownValue {

    @NotNull GuiValue stateValue();

    record TextValue(@NotNull String value) implements GuiDialogFieldValue {
        @Override
        public @NotNull GuiValue stateValue() {
            return GuiValue.of(value);
        }
    }

    record ToggleValue(boolean value) implements GuiDialogFieldValue {
        @Override
        public @NotNull GuiValue stateValue() {
            return GuiValue.of(value);
        }
    }

    record SliderValue(float value) implements GuiDialogFieldValue {
        @Override
        public @NotNull GuiValue stateValue() {
            return GuiValue.of(value);
        }
    }

    record DropdownValue(@NotNull String selectedId, @Nullable Option selectedOption) implements GuiDialogFieldValue {
        @Override
        public @NotNull GuiValue stateValue() {
            return GuiValue.of(selectedId);
        }

        public @Nullable GuiValue selectedData() {
            return selectedOption != null ? selectedOption.data() : null;
        }
    }
}
