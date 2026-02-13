package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static boolean isInitialized() {
        return bus != null;
    }
}
