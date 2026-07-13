package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.objects.RLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Utility enforcing finite coordinate semantics for entity locations. */
final class SharedLocationSemantics {
    private SharedLocationSemantics() {
    }

    static void requireFinite(@NotNull RLocation location) {
        Objects.requireNonNull(location, "location");
        requireFinite(location.x(), "x");
        requireFinite(location.y(), "y");
        requireFinite(location.z(), "z");
        requireFinite(location.yaw(), "yaw");
        requireFinite(location.pitch(), "pitch");
    }

    static void apply(@NotNull Entity entity, @NotNull RLocation location) {
        Objects.requireNonNull(entity, "entity");
        requireFinite(location);
        entity.setPos(location.x(), location.y(), location.z());
        entity.setYRot(location.yaw());
        entity.setXRot(location.pitch());
    }

    private static void requireFinite(double value, @NotNull String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireFinite(float value, @NotNull String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
