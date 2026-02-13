package de.t14d3.rapunzellib.gui.core;

import de.t14d3.rapunzellib.gui.Gui;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.layout.GridLayout;
import de.t14d3.rapunzellib.gui.layout.GuiLayout;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public record GuiSlotPlan(int rows, int size, @NotNull Map<Integer, GuiElement> slots) {
    public GuiSlotPlan {
        rows = Math.max(1, Math.min(6, rows));
        size = rows * 9;
        slots = Map.copyOf(slots);
    }

    public static @NotNull GuiSlotPlan resolve(@NotNull Gui gui, int fallbackRows) {
        return resolve(gui.layout(), gui.rows(), fallbackRows);
    }

    public static @NotNull GuiSlotPlan resolve(@NotNull GuiLayout layout, int explicitRows, int fallbackRows) {
        if (layout instanceof GridLayout gridLayout) {
            int rows = explicitRows > 0 ? explicitRows : gridLayout.rows() > 0 ? gridLayout.rows() : fallbackRows;
            return new GuiSlotPlan(rows, rows * 9, gridLayout.slots());
        }

        Map<Integer, GuiElement> slots = new LinkedHashMap<>();
        int slot = 0;
        for (GuiElement element : layout.elements()) {
            if (element != null) {
                slots.put(slot, element);
            }
            slot++;
        }

        int rows = Math.max(1, Math.min(6, (int) Math.ceil(Math.max(layout.elements().size(), 1) / 9.0)));
        return new GuiSlotPlan(rows, rows * 9, slots);
    }
}
