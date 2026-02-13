package de.t14d3.rapunzellib.gui.context;

import de.t14d3.rapunzellib.gui.value.GuiValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface GuiState {
    @NotNull Map<String, GuiValue> values();

    @Nullable GuiValue value(@NotNull String key);
    
    @Nullable <T> T get(@NotNull String key, @NotNull Class<T> type);
    
    default <T> @NotNull T get(@NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
        T value = get(key, type);
        return value != null ? value : defaultValue;
    }
    
    void set(@NotNull String key, @Nullable GuiValue value);

    default void setString(@NotNull String key, @NotNull String value) {
        set(key, GuiValue.of(value));
    }

    default void setBoolean(@NotNull String key, boolean value) {
        set(key, GuiValue.of(value));
    }

    default void setNumber(@NotNull String key, double value) {
        set(key, GuiValue.of(value));
    }
    
    default boolean has(@NotNull String key) {
        return values().containsKey(key);
    }
    
    default void remove(@NotNull String key) {
        set(key, null);
    }
    
    default void clear() {
        values().clear();
    }
    
    @NotNull
    static GuiState create() {
        return new DefaultGuiState();
    }
}

class DefaultGuiState implements GuiState {
    private final Map<String, GuiValue> values = new HashMap<>();
    
    @Override
    public @NotNull Map<String, GuiValue> values() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public @Nullable GuiValue value(@NotNull String key) {
        return values.get(key);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(@NotNull String key, @NotNull Class<T> type) {
        GuiValue value = values.get(key);
        if (value == null) {
            return null;
        }
        if (type == String.class) {
            return (T) value.stringValue();
        }
        if (type == Boolean.class) {
            return (T) value.booleanValue();
        }
        if (type == Float.class) {
            Double number = value.numberValue();
            return number != null ? (T) Float.valueOf(number.floatValue()) : null;
        }
        if (type == Double.class) {
            return (T) value.numberValue();
        }
        if (type == Integer.class) {
            Double number = value.numberValue();
            return number != null ? (T) Integer.valueOf(number.intValue()) : null;
        }
        if (type == Long.class) {
            Double number = value.numberValue();
            return number != null ? (T) Long.valueOf(number.longValue()) : null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    @Override
    public void set(@NotNull String key, @Nullable GuiValue value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }
}
