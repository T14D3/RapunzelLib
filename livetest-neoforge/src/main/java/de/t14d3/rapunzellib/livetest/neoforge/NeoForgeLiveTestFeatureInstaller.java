package de.t14d3.rapunzellib.livetest.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.livetest.shared.AbstractSharedLiveTestFeatureInstaller;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * NeoForge-specific implementation of {@link de.t14d3.rapunzellib.livetest.LiveTestFeatureInstaller}.
 * <p>
 * Registers the live test host and bot service services for the NeoForge platform.
 * Uses the console-based fallback (external bot process via stdout protocol)
 * for bot management.
 * Command registration is not yet implemented.
 * </p>
 */
public final class NeoForgeLiveTestFeatureInstaller extends AbstractSharedLiveTestFeatureInstaller {

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");
        super.install(context);
    }
}