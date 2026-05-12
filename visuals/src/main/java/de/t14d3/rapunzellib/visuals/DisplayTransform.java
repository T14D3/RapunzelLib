package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

/**
 * A display transformation composed of translation, scale, and left/right rotations.
 * <p>
 * Used by {@link BlockDisplayConfig} to position and orient block display entities.
 *
 * @param translation  the translation offset
 * @param scale        the scale factor
 * @param leftRotation the left (pre) rotation quaternion
 * @param rightRotation the right (post) rotation quaternion
 */
public record DisplayTransform(
    @NotNull Vector3f translation,
    @NotNull Vector3f scale,
    @NotNull Quaternionf leftRotation,
    @NotNull Quaternionf rightRotation
) {

    /**
     * Returns the identity transform (no translation, unit scale, no rotation).
     *
     * @return the identity display transform
     */
    public static @NotNull DisplayTransform identity() {
        return new DisplayTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(1, 1, 1),
            new Quaternionf(),
            new Quaternionf()
        );
    }

    /**
     * Creates a scale-only transform.
     *
     * @param sx the x-axis scale factor
     * @param sy the y-axis scale factor
     * @param sz the z-axis scale factor
     * @return the scale display transform
     */
    public static @NotNull DisplayTransform scale(float sx, float sy, float sz) {
        return new DisplayTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(sx, sy, sz),
            new Quaternionf(),
            new Quaternionf()
        );
    }

    /**
     * Creates a translation-only transform.
     *
     * @param x the x-axis translation
     * @param y the y-axis translation
     * @param z the z-axis translation
     * @return the translation display transform
     */
    public static @NotNull DisplayTransform translate(float x, float y, float z) {
        return new DisplayTransform(
            new Vector3f(x, y, z),
            new Vector3f(1, 1, 1),
            new Quaternionf(),
            new Quaternionf()
        );
    }
}
