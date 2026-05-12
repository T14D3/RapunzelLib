package de.t14d3.rapunzellib.gui.shared.dialog;

import de.t14d3.rapunzellib.gui.value.GuiValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Represents a submitted dialog with a map of field values.
 *
 * @param values the submitted values keyed by field key
 */
public record SharedDialogSubmission(@NotNull Map<String, GuiValue> values) {
    /**
     * Creates a submission with a defensive copy of the values map.
     *
     * @param values the submitted values
     */
    public SharedDialogSubmission {
        values = Map.copyOf(values);
    }

    /**
     * Returns an empty submission.
     *
     * @return an empty submission
     */
    public static @NotNull SharedDialogSubmission empty() {
        return new SharedDialogSubmission(Map.of());
    }

    /**
     * Returns the value for a given key.
     *
     * @param key the field key
     * @return the value, or {@code null} if not present
     */
    public @Nullable GuiValue value(@NotNull String key) {
        return values.get(key);
    }
}
