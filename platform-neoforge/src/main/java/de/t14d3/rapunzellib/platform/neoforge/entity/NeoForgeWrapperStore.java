package de.t14d3.rapunzellib.platform.neoforge.entity;

import de.t14d3.rapunzellib.platform.shared.entity.NmsWrapperStore;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge-specific {@link NmsWrapperStore}.
 *
 * <p>Caches {@link de.t14d3.rapunzellib.objects.RWorldRef} by NMS
 * {@code ServerLevel}. NeoForge has no dedicated native location type, so
 * {@link #location(Object)} always returns an empty {@link java.util.Optional}
 * and callers must build an {@link de.t14d3.rapunzellib.objects.RLocation}
 * from coordinates.</p>
 */
public final class NeoForgeWrapperStore extends NmsWrapperStore {
    @Override
    public @NotNull String toString() {
        return "NeoForgeWrapperStore";
    }
}
