package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Abstract base implementation of {@link GameEventBridgeInstaller} that stores
 * the platform ID and support manifest.
 *
 * <p>Subclasses need only implement {@link #installBridge(RapunzelContext, GameEventBus)}
 * to provide the platform-specific hook installation logic.</p>
 */
public abstract class AbstractGameEventBridgeInstaller implements GameEventBridgeInstaller {
    private final PlatformId platformId;
    private final GameEventSupportManifest supportManifest;

    /**
     * Constructs a new abstract bridge installer.
     *
     * @param platformId      the platform identifier
     * @param supportManifest the support manifest for this platform
     */
    protected AbstractGameEventBridgeInstaller(
        @NotNull PlatformId platformId,
        @NotNull GameEventSupportManifest supportManifest
    ) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.supportManifest = Objects.requireNonNull(supportManifest, "supportManifest");
    }

    @Override
    public final @NotNull PlatformId platformId() {
        return platformId;
    }

    @Override
    public final @NotNull GameEventSupportManifest supportManifest() {
        return supportManifest;
    }

    /**
     * Installs the platform bridge by delegating to {@link #installBridge}.
     *
     * @param context the Rapunzel context
     * @param bus     the game event bus
     * @return the installed bridge
     */
    @Override
    public final @NotNull GameEventBridge install(@NotNull RapunzelContext context, @NotNull GameEventBus bus) {
        return installBridge(context, bus);
    }

    /**
     * Installs the platform-specific bridge. Implemented by subclasses.
     *
     * @param context the Rapunzel context
     * @param bus     the game event bus
     * @return the installed bridge
     */
    protected abstract @NotNull GameEventBridge installBridge(@NotNull RapunzelContext context, @NotNull GameEventBus bus);
}
