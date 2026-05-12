package de.t14d3.rapunzellib.visuals;

/**
 * A quaternion with float components, representing a rotation.
 * <p>
 * The default constructor creates the identity quaternion (no rotation).
 *
 * @param x the x-component
 * @param y the y-component
 * @param z the z-component
 * @param w the w-component (scalar part)
 */
public record Quaternionf(float x, float y, float z, float w) {

    /**
     * Creates the identity quaternion (0, 0, 0, 1).
     */
    public Quaternionf() {
        this(0, 0, 0, 1);
    }
}
