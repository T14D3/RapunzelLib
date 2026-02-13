package de.t14d3.rapunzellib.gui.dialog;

import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class GuiDialogSubmissionProcessor {
    private GuiDialogSubmissionProcessor() {
    }

    public static void submit(
        @NotNull GuiDialogModel model,
        @NotNull Map<String, GuiValue> values,
        @NotNull RPlayer player,
        @NotNull GuiState state
    ) {
        submit(model, GuiDialogStateSupport.collectSubmittedValues(model, field -> values.get(field.key())), player, state);
    }

    public static void submit(
        @NotNull GuiDialogModel model,
        @NotNull GuiDialogFieldValues values,
        @NotNull RPlayer player,
        @NotNull GuiState state
    ) {
        for (GuiDialogField field : model.interactiveFields()) {
            if (values.contains(field.key())) {
                field.submit(values.value(field.key()), player, state);
            }
        }
    }

    public static void submit(
        @NotNull GuiDialogField field,
        @Nullable GuiDialogFieldValue value,
        @NotNull RPlayer player,
        @NotNull GuiState state
    ) {
        field.submit(value, player, state);
    }
}
