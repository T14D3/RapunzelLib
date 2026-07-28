package de.t14d3.rapunzellib.platform.fabric.entity;

import de.t14d3.rapunzellib.platform.shared.entity.NmsWrapperStore;
import org.jetbrains.annotations.NotNull;

/**
 * Fabric-specific {@link NmsWrapperStore}.
 *
 * <p>Caches {@link de.t14d3.rapunzellib.objects.RWorldRef} by NMS
 * {@code ServerLevel}. Fabric has no dedicated native location type (only
 * {@code Vec3}, which carries no world or rotation), so
 * {@link #location(Object)} always returns an empty {@link java.util.Optional}
 * and callers must build an {@link de.t14d3.rapunzellib.objects.RLocation}
 * from coordinates.</p>
 */
public final class FabricWrapperStore extends NmsWrapperStore {
    @Override
    public @NotNull String toString() {
        return "FabricWrapperStore";
    }
}
