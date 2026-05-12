package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

/**
 * Installs a platform-specific {@link GameEventBridge} for a given platform.
 *
 * <p>Each platform implementation provides an installer that knows how to
 * hook into the platform's native event system and translate events into
 * RapunzelLib {@link GameEvent} instances dispatched via the {@link GameEventBus}.</p>
 */
public interface GameEventBridgeInstaller {
    /**
     * Returns the platform ID this installer targets.
     *
     * @return the platform identifier
     */
    @NotNull PlatformId platformId();

    default @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifest.empty(platformId());
    }

    /**
     * Installs platform hooks and connects them to {@code bus}.
     */
    @NotNull GameEventBridge install(@NotNull RapunzelContext context, @NotNull GameEventBus bus);
}
