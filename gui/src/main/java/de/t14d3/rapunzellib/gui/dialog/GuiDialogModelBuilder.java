package de.t14d3.rapunzellib.gui.dialog;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.element.ButtonElement;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GuiDialogModelBuilder {
    private GuiDialogModelBuilder() {
    }

    public static @NotNull GuiDialogModel build(@NotNull Gui gui, @NotNull Component defaultTitle) {
        List<GuiElement> elements = new ArrayList<>();
        List<GuiDialogField> interactiveFields = new ArrayList<>();
        List<ButtonElement> buttons = new ArrayList<>();
        Map<String, GuiDialogField> keyedFields = new LinkedHashMap<>();

        for (GuiElement element : gui.layout().elements()) {
            if (element == null) {
                continue;
            }
            elements.add(element);

            if (element instanceof ButtonElement button) {
                buttons.add(button);
                continue;
            }

            GuiDialogField field = GuiDialogField.of(element);
            if (field != null) {
                registerInteractiveField(interactiveFields, keyedFields, field);
            }
        }

        return new GuiDialogModel(gui.title() != null ? gui.title() : defaultTitle, elements, interactiveFields, buttons, keyedFields);
    }

    private static void registerInteractiveField(
        @NotNull List<GuiDialogField> interactiveFields,
        @NotNull Map<String, GuiDialogField> keyedFields,
        @NotNull GuiDialogField field
    ) {
        interactiveFields.add(field);
        keyedFields.put(field.key(), field);
    }
}
