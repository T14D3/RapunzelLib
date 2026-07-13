package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-safe holder for the {@link GameEventBus} used by mixin hooks.
 * <p>
 * Accessed by both the shared bridge and platform-specific mixin code.
 */
public final class SharedMixinBusHolder {
    private static @Nullable GameEventBus bus;

    private SharedMixinBusHolder() {
    }

    public static void setBus(@NotNull GameEventBus eventBus) {
        bus = eventBus;
    }

    public static @Nullable GameEventBus bus() {
        return bus;
    }

    /**
     * Checks whether the bus has been initialized.
     *
     * @return {@code true} if the bus is set
     */
    public static boolean isInitialized() {
        return bus != null;
    }
}
