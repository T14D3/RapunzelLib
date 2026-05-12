package de.t14d3.rapunzellib.gui;

/**
 * Capabilities that a GUI renderer may support.
 * <p>
 * Each value represents a feature that a platform-specific renderer can advertise,
 * allowing the system to select an appropriate renderer for a given GUI.
 * </p>
 */
public enum GuiCapability {
    NATIVE_TEXT_INPUT,
    NATIVE_SLIDER,
    NATIVE_TOGGLE,
    NATIVE_DROPDOWN,
    GRID_LAYOUT,
    ANIMATIONS,
    ITEM_DISPLAY,
    PAGINATION,
    MODAL,
    SCROLLABLE
}
