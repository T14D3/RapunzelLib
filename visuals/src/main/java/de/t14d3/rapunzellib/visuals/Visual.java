package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all visual effects in RapunzelLib.
 * <p>
 * A visual is a renderable object that can be shown to players, hidden, or permanently removed.
 * Each visual carries an identifier, a typed configuration, and an audience specification.
 *
 * @param <C> the type of visual configuration this visual uses
 */
public interface Visual<C extends VisualConfig> {

    /**
     * Returns the unique identifier of this visual.
     *
     * @return the visual ID
     */
    @NotNull VisualId id();

    /**
     * Returns the configuration of this visual.
     *
     * @return the visual configuration
     */
    @NotNull C config();

    /**
     * Returns the audience specification for this visual.
     *
     * @return the visual audience
     */
    @NotNull VisualAudience audience();

    /**
     * Shows this visual to its intended audience.
     */
    void show();

    /**
     * Hides this visual without removing it permanently.
     */
    void hide();

    /**
     * Hides and permanently removes this visual, freeing associated resources.
     */
    void remove();

    /**
     * Checks whether this visual is currently being shown.
     *
     * @return {@code true} if the visual is currently shown, {@code false} otherwise
     */
    boolean isShown();
}
