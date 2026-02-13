package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import org.jetbrains.annotations.NotNull;

public final class EventFeatures {
    private EventFeatures() {
    }

    public static @NotNull GameEventBus install() {
        return GameEvents.install();
    }

    public static @NotNull GameEventBus install(@NotNull RapunzelContext context) {
        return GameEvents.install(context);
    }

    public static @NotNull GameEventBus bus() {
        return GameEvents.bus();
    }

    public static @NotNull GameEventSupportManifest support() {
        return GameEvents.support();
    }

    public static @NotNull GameEventSupportManifest support(@NotNull PlatformRuntime runtime) {
        return GameEvents.support(runtime);
    }

    public static @NotNull GameEventSupportManifest support(@NotNull PlatformId platformId) {
        return GameEvents.support(platformId);
    }
}
