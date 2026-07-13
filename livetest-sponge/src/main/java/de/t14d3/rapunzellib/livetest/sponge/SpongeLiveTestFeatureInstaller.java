package de.t14d3.rapunzellib.livetest.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.livetest.shared.AbstractSharedLiveTestFeatureInstaller;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Sponge-specific implementation of {@link de.t14d3.rapunzellib.livetest.LiveTestFeatureInstaller}.
 * <p>
 * Registers the live test host and bot service services for the Sponge platform.
 * Uses the console-based fallback (external bot process via stdout protocol)
 * for bot management.
 * Command registration is not yet implemented.
 * </p>
 */
public final class SpongeLiveTestFeatureInstaller extends AbstractSharedLiveTestFeatureInstaller {

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.SPONGE;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");
        super.install(context);
    }
}