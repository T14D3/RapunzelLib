package de.t14d3.rapunzellib.gui.context;

import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ClickContext {
    @NotNull RPlayer player();
    
    @NotNull GuiElement element();
    
    int slot();
    
    @NotNull ClickType clickType();
    
    /**
     * Get the GUI state containing all input values.
     * This allows buttons to access values from inputs, toggles, sliders, etc.
     * @return the GUI state
     */
    @NotNull GuiState state();

    default @Nullable GuiValue value(@NotNull String key) {
        return state().value(key);
    }
    
    /**
     * Convenience method to get a value from the state.
     * @param key the input key
     * @param type the expected type
     * @return the value, or null if not found
     */
    default <T> @Nullable T get(@NotNull String key, @NotNull Class<T> type) {
        return state().get(key, type);
    }
    
    /**
     * Convenience method to get a value from the state with a default.
     * @param key the input key
     * @param type the expected type
     * @param defaultValue the default value if not found
     * @return the value, or the default if not found
     */
    default <T> @NotNull T get(@NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
        return state().get(key, type, defaultValue);
    }
}
