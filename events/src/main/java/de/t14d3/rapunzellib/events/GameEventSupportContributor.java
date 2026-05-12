package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

/**
 * Service provider interface for contributing to the {@link GameEventSupportManifest}
 * of a platform.
 *
 * <p>Implementations are loaded via {@link java.util.ServiceLoader} and can mark
 * specific event types as unsupported for a given platform, allowing them to
 * override the default installer manifest.</p>
 */
public interface GameEventSupportContributor {
    /**
     * Returns the platform ID this contributor applies to.
     *
     * @return the platform identifier
     */
    @NotNull PlatformId platformId();

    /**
     * Returns a {@link GameEventSupportManifest} marking which events are unsupported
     * by this contributor's platform.
     *
     * @return the support manifest (empty by default)
     */
    default @NotNull GameEventSupportManifest supportManifest() {
        return GameEventSupportManifest.empty(platformId());
    }
}
