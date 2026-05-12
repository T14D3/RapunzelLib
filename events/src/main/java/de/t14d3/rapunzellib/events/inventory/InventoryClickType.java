package de.t14d3.rapunzellib.events.inventory;

/**
 * Types of inventory click actions that can be performed.
 *
 * <p>Includes standard clicks (LEFT, RIGHT), modifier clicks (SHIFT_LEFT, SHIFT_RIGHT),
 * special actions (MIDDLE, DOUBLE_CLICK, DROP, CONTROL_DROP),
 * hotbar selection (NUMBER_KEY_1), offhand swap (SWAP_OFFHAND), and
 * a fallback (UNKNOWN) for unrecognized click types.</p>
 */
public enum InventoryClickType {
    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    MIDDLE,
    DOUBLE_CLICK,
    DROP,
    CONTROL_DROP,
    NUMBER_KEY_1,
    SWAP_OFFHAND,
    UNKNOWN
}
