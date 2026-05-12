package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

/**
 * Context available during GUI rendering.
 * <p>
 * Provides access to the player, the GUI, the current state, and the element
 * registry for the current render pass.
 * </p>
 */
public interface RenderContext {
    /**
     * Returns the player for whom the GUI is being rendered.
     *
     * @return the player
     */
    @NotNull RPlayer player();

    /**
     * Returns the GUI being rendered.
     *
     * @return the GUI
     */
    @NotNull Gui gui();

    /**
     * Returns the current state of the GUI.
     *
     * @return the GUI state
     */
    @NotNull GuiState state();

    /**
     * Returns the element at the given slot, or null if empty.
     *
     * @param slot the slot index
     * @return the element, or null
     */
    @Nullable GuiElement elementAt(int slot);

    /**
     * Returns the full element registry mapping slots to elements.
     *
     * @return an unmodifiable map of slot to element
     */
    @NotNull Map<Integer, GuiElement> elementRegistry();

    default @Nullable GuiValue value(@NotNull String key) {
        return state().value(key);
    }
    
    /**
     * Registers an element at a specific slot.
     *
     * @param slot    the slot index
     * @param element the element to register
     */
    void registerElement(int slot, @NotNull GuiElement element);
    
    default <T> @NotNull T get(@NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
        return state().get(key, type, defaultValue);
    }
    
    default <T> @Nullable T get(@NotNull String key, @NotNull Class<T> type) {
        return state().get(key, type);
    }
    
    /**
     * Stores a value in the GUI state.
     *
     * @param key   the key
     * @param value the value to store
     */
    void set(@NotNull String key, @NotNull GuiValue value);

    /**
     * Stores a string value in the GUI state.
     *
     * @param key   the key
     * @param value the string value
     */
    default void setString(@NotNull String key, @NotNull String value) {
        set(key, GuiValue.of(value));
    }

    /**
     * Stores a boolean value in the GUI state.
     *
     * @param key   the key
     * @param value the boolean value
     */
    default void setBoolean(@NotNull String key, boolean value) {
        set(key, GuiValue.of(value));
    }

    /**
     * Stores a numeric value in the GUI state.
     *
     * @param key   the key
     * @param value the numeric value
     */
    default void setNumber(@NotNull String key, double value) {
        set(key, GuiValue.of(value));
    }
}
