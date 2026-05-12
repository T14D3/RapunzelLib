package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.objects.RLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Utility enforcing finite coordinate semantics for entity locations.
 * <p>
 * Validates that all components of an {@link RLocation} (x, y, z, yaw, pitch)
 * are finite numbers, and applies position and rotation values to a native {@link Entity}.
 * </p>
 */
final class SharedLocationSemantics {
    private SharedLocationSemantics() {
    }

    /**
     * Validates that all components of the given location are finite (not NaN or infinity).
     *
     * @param location the location to validate
     * @throws IllegalArgumentException if any coordinate is not finite
     */
    static void requireFinite(@NotNull RLocation location) {
        Objects.requireNonNull(location, "location");
        requireFinite(location.x(), "x");
        requireFinite(location.y(), "y");
        requireFinite(location.z(), "z");
        requireFinite(location.yaw(), "yaw");
        requireFinite(location.pitch(), "pitch");
    }

    /**
     * Applies the given location's position and rotation to the entity.
     *
     * @param entity   the native entity to move
     * @param location the location containing coordinates and rotation
     * @throws IllegalArgumentException if any coordinate is not finite
     */
    static void apply(@NotNull Entity entity, @NotNull RLocation location) {
        Objects.requireNonNull(entity, "entity");
        requireFinite(location);
        entity.setPos(location.x(), location.y(), location.z());
        entity.setYRot(location.yaw());
        entity.setXRot(location.pitch());
    }

    /**
     * Validates that a double value is finite.
     *
     * @param value the value to check
     * @param name  the parameter name for the error message
     * @throws IllegalArgumentException if the value is not finite
     */
    private static void requireFinite(double value, @NotNull String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    /**
     * Validates that a float value is finite.
     *
     * @param value the value to check
     * @param name  the parameter name for the error message
     * @throws IllegalArgumentException if the value is not finite
     */
    private static void requireFinite(float value, @NotNull String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
