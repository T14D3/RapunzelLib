package de.t14d3.rapunzellib.gui.dialog;

import de.t14d3.rapunzellib.gui.element.ButtonElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record GuiDialogModel(
    @NotNull Component title,
    @NotNull List<GuiElement> elements,
    @NotNull List<GuiDialogField> interactiveFields,
    @NotNull List<ButtonElement> buttons,
    @NotNull Map<String, GuiDialogField> keyedFields
) {
    public GuiDialogModel {
        elements = List.copyOf(elements);
        interactiveFields = List.copyOf(interactiveFields);
        buttons = List.copyOf(buttons);
        keyedFields = Map.copyOf(keyedFields);
    }
}
