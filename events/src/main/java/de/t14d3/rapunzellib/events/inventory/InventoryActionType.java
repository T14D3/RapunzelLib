package de.t14d3.rapunzellib.events.inventory;

/**
 * Types of inventory actions that can be performed.
 *
 * <p>Faithful to the Bukkit {@code ClickType} set: {@link #LEFT},
 * {@link #RIGHT}, {@link #SHIFT_LEFT}, {@link #SHIFT_RIGHT},
 * {@link #MIDDLE}, {@link #DOUBLE_CLICK}, {@link #DROP},
 * {@link #CONTROL_DROP}, {@link #NUMBER_KEY}, {@link #CREATIVE} and
 * {@link #SWAP_OFFHAND} map one-to-one to their Bukkit counterparts,
 * {@link #DRAG} covers drags (from {@code InventoryDragEvent}), and
 * {@link #UNKNOWN} is the fallback for unmapped platform actions. The
 * fine-grained {@link InventoryClickType} enum remains available for GUI
 * renderers.</p>
 */
public enum InventoryActionType {
    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    MIDDLE,
    NUMBER_KEY,
    DOUBLE_CLICK,
    DROP,
    CONTROL_DROP,
    CREATIVE,
    SWAP_OFFHAND,
    DRAG,
    UNKNOWN,
}
