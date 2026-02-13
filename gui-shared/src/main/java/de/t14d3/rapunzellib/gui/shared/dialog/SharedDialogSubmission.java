package de.t14d3.rapunzellib.gui.shared.dialog;

import de.t14d3.rapunzellib.gui.value.GuiValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record SharedDialogSubmission(@NotNull Map<String, GuiValue> values) {
    public SharedDialogSubmission {
        values = Map.copyOf(values);
    }

    public static @NotNull SharedDialogSubmission empty() {
        return new SharedDialogSubmission(Map.of());
    }

    public @Nullable GuiValue value(@NotNull String key) {
        return values.get(key);
    }
}
