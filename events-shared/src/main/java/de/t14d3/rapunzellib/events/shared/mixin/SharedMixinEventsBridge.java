package de.t14d3.rapunzellib.events.shared.mixin;

import de.t14d3.rapunzellib.events.GameEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bridge that initializes and provides access to the shared {@link GameEventBus}
 * for mixin-based event dispatch.
 * <p>
 * Delegates to {@link SharedMixinBusHolder} for storage.
 */
public final class SharedMixinEventsBridge {
    private SharedMixinEventsBridge() {
    }

    /**
     * Initializes the bridge with the given event bus.
     *
     * @param eventBus the game event bus
     */
    public static void initialize(@NotNull GameEventBus eventBus) {
        SharedMixinBusHolder.setBus(eventBus);
    }

    public static @Nullable GameEventBus bus() {
        return SharedMixinBusHolder.bus();
    }

    /**
     * Checks whether the bridge has been initialized.
     *
     * @return {@code true} if initialized
     */
    public static boolean isInitialized() {
        return SharedMixinBusHolder.isInitialized();
    }
}
