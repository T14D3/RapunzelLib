package de.t14d3.rapunzellib.gui;

import de.t14d3.rapunzellib.gui.context.GuiState;
import de.t14d3.rapunzellib.gui.element.GuiElement;
import de.t14d3.rapunzellib.gui.value.GuiValue;
import de.t14d3.rapunzellib.objects.RPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public interface RenderContext {
    @NotNull RPlayer player();
    
    @NotNull Gui gui();
    
    @NotNull GuiState state();
    
    @Nullable GuiElement elementAt(int slot);
    
    @NotNull Map<Integer, GuiElement> elementRegistry();

    default @Nullable GuiValue value(@NotNull String key) {
        return state().value(key);
    }
    
    void registerElement(int slot, @NotNull GuiElement element);
    
    default <T> @NotNull T get(@NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
        return state().get(key, type, defaultValue);
    }
    
    default <T> @Nullable T get(@NotNull String key, @NotNull Class<T> type) {
        return state().get(key, type);
    }
    
    void set(@NotNull String key, @NotNull GuiValue value);

    default void setString(@NotNull String key, @NotNull String value) {
        set(key, GuiValue.of(value));
    }

    default void setBoolean(@NotNull String key, boolean value) {
        set(key, GuiValue.of(value));
    }

    default void setNumber(@NotNull String key, double value) {
        set(key, GuiValue.of(value));
    }
}
