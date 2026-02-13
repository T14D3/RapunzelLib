package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SharedMixinEventsBridge {
    private SharedMixinEventsBridge() {
    }

    public static void initialize(@NotNull GameEventBus eventBus) {
        SharedMixinBusHolder.setBus(eventBus);
    }

    public static @Nullable GameEventBus bus() {
        return SharedMixinBusHolder.bus();
    }

    public static boolean isInitialized() {
        return SharedMixinBusHolder.isInitialized();
    }
}
