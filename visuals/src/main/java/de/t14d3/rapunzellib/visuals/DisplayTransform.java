package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

public record DisplayTransform(
    @NotNull Vector3f translation,
    @NotNull Vector3f scale,
    @NotNull Quaternionf leftRotation,
    @NotNull Quaternionf rightRotation
) {
    public static @NotNull DisplayTransform identity() {
        return new DisplayTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(1, 1, 1),
            new Quaternionf(),
            new Quaternionf()
        );
    }

    public static @NotNull DisplayTransform scale(float sx, float sy, float sz) {
        return new DisplayTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(sx, sy, sz),
            new Quaternionf(),
            new Quaternionf()
        );
    }

    public static @NotNull DisplayTransform translate(float x, float y, float z) {
        return new DisplayTransform(
            new Vector3f(x, y, z),
            new Vector3f(1, 1, 1),
            new Quaternionf(),
            new Quaternionf()
        );
    }
}
