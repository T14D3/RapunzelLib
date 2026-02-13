package de.t14d3.rapunzellib.gui.dialog;

import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GuiDialogStateSupport {
    private GuiDialogStateSupport() {
    }

    public static @NotNull GuiDialogFieldValue currentValue(@NotNull GuiDialogField field, @NotNull GuiState state) {
        return field.currentValue(state);
    }

    public static @NotNull GuiDialogFieldValues collectSubmittedValues(
        @NotNull GuiDialogModel model,
        @NotNull ValueExtractor extractor
    ) {
        Map<String, GuiDialogFieldValue> values = new LinkedHashMap<>();
        for (GuiDialogField field : model.interactiveFields()) {
            GuiValue value = extractor.extract(field);
            if (value != null) {
                values.put(field.key(), field.submittedValue(value));
            }
        }
        return new GuiDialogFieldValues(values);
    }

    @FunctionalInterface
    public interface ValueExtractor {
        @Nullable GuiValue extract(@NotNull GuiDialogField field);
    }
}
