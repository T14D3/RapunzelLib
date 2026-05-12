package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import org.jetbrains.annotations.NotNull;

/**
 * Convenience entry point for accessing the event system features.
 *
 * <p>Delegates all calls to {@link GameEvents}. This class exists as a thin
 * facade for discoverability in the RapunzelLib feature API.</p>
 */
public final class EventFeatures {
    private EventFeatures() {
    }

    /**
     * Installs and returns the {@link GameEventBus}.
     *
     * @return the installed event bus
     */
    public static @NotNull GameEventBus install() {
        return GameEvents.install();
    }

    /**
     * Installs and returns the {@link GameEventBus} using the given context.
     *
     * @param context the Rapunzel context
     * @return the installed event bus
     */
    public static @NotNull GameEventBus install(@NotNull RapunzelContext context) {
        return GameEvents.install(context);
    }

    /**
     * Returns the installed {@link GameEventBus}.
     *
     * @return the event bus
     */
    public static @NotNull GameEventBus bus() {
        return GameEvents.bus();
    }

    /**
     * Returns the support manifest for the current platform.
     *
     * @return the support manifest
     */
    public static @NotNull GameEventSupportManifest support() {
        return GameEvents.support();
    }

    /**
     * Returns the support manifest for the given runtime.
     *
     * @param runtime the platform runtime
     * @return the support manifest
     */
    public static @NotNull GameEventSupportManifest support(@NotNull PlatformRuntime runtime) {
        return GameEvents.support(runtime);
    }

    /**
     * Returns the support manifest for the given platform ID.
     *
     * @param platformId the platform identifier
     * @return the support manifest
     */
    public static @NotNull GameEventSupportManifest support(@NotNull PlatformId platformId) {
        return GameEvents.support(platformId);
    }
}
