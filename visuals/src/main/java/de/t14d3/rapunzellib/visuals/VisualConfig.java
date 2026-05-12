package de.t14d3.rapunzellib.visuals;

/**
 * Base interface for all visual configuration records.
 * <p>
 * Every visual configuration must specify a view distance
 * that limits how far away players can see the visual.
 */
public interface VisualConfig {

    /**
     * Returns the maximum view distance in blocks.
     *
     * @return the view distance; a negative value means no limit
     */
    double viewDistance();
}
