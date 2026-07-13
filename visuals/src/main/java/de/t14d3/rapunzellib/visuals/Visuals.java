package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

/**
 * Entry point for accessing the visuals system.
 * <p>
 * Provides access to the {@link VisualManager} and a convenient
 * {@link VisualBuilder} for constructing visual objects.
 */
public interface Visuals {

    @NotNull VisualManager manager();

    /**
     * Returns a new visual builder backed by this system's manager.
     *
     * @return a visual builder
     */
    @NotNull
    default VisualBuilder builder() {
        return new VisualBuilder(manager());
    }
}
