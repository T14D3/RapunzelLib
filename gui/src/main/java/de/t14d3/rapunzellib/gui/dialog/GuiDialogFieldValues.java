package de.t14d3.rapunzellib.gui.dialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record GuiDialogFieldValues(@NotNull Map<String, GuiDialogFieldValue> values) {
    public GuiDialogFieldValues {
        values = Map.copyOf(values);
    }

    public static @NotNull GuiDialogFieldValues empty() {
        return new GuiDialogFieldValues(Map.of());
    }

    public boolean contains(@NotNull String key) {
        return values.containsKey(key);
    }

    public @Nullable GuiDialogFieldValue value(@NotNull String key) {
        return values.get(key);
    }
}
