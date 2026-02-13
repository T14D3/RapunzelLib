package de.t14d3.rapunzellib.gui.core;

import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.element.DropdownElement;
import de.t14d3.rapunzellib.gui.element.InputElement;
import de.t14d3.rapunzellib.gui.element.Option;
import de.t14d3.rapunzellib.gui.element.SliderElement;
import de.t14d3.rapunzellib.gui.element.ToggleElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GuiElementStates {
    private GuiElementStates() {
    }

    public static boolean toggleValue(@NotNull ToggleElement toggle, @NotNull GuiState state) {
        return state.get(toggle.key(), Boolean.class, toggle.defaultValue());
    }

    public static float sliderValue(@NotNull SliderElement slider, @NotNull GuiState state) {
        return state.get(slider.key(), Float.class, slider.defaultValue());
    }

    public static int sliderPercent(@NotNull SliderElement slider, @NotNull GuiState state) {
        float range = slider.max() - slider.min();
        if (range <= 0.0f) {
            return 100;
        }
        float progress = (sliderValue(slider, state) - slider.min()) / range;
        return Math.max(0, Math.min(100, Math.round(progress * 100.0f)));
    }

    public static @NotNull String inputValue(@NotNull InputElement input, @NotNull GuiState state) {
        return state.get(input.key(), String.class, input.defaultValue() != null ? input.defaultValue() : "");
    }

    public static @NotNull DropdownState dropdown(@NotNull DropdownElement dropdown, @NotNull GuiState state) {
        String defaultId = dropdown.defaultValue() != null ? dropdown.defaultValue().id() : "";
        String selectedId = state.get(dropdown.key(), String.class, defaultId);
        Option selectedOption = GuiInteractionEngine.resolveOption(dropdown, selectedId);
        return new DropdownState(selectedId, selectedOption);
    }

    public static @NotNull String progressBar(int filledBars, int totalBars, char filled, char empty) {
        int clampedTotal = Math.max(1, totalBars);
        int clampedFilled = Math.max(0, Math.min(clampedTotal, filledBars));
        StringBuilder builder = new StringBuilder(clampedTotal);
        for (int i = 0; i < clampedFilled; i++) {
            builder.append(filled);
        }
        for (int i = clampedFilled; i < clampedTotal; i++) {
            builder.append(empty);
        }
        return builder.toString();
    }

    public record DropdownState(@NotNull String selectedId, @Nullable Option selectedOption) {
    }
}
