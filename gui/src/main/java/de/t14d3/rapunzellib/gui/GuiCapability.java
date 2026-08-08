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
    SCROLLABLE,
    /**
     * The renderer draws GUI content onto a 128x128 map item canvas instead of an inventory.
     */
    MAP_RENDERING,
    /**
     * The renderer can display live world terrain as the map background.
     */
    LIVE_TERRAIN,
    /**
     * The renderer delivers clicks with pixel and block coordinates.
     */
    PIXEL_INPUT
}
