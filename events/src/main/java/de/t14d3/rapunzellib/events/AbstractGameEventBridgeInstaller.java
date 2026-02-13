package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class AbstractGameEventBridgeInstaller implements GameEventBridgeInstaller {
    private final PlatformId platformId;
    private final GameEventSupportManifest supportManifest;

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

    @Override
    public final @NotNull GameEventBridge install(@NotNull RapunzelContext context, @NotNull GameEventBus bus) {
        return installBridge(context, bus);
    }

    protected abstract @NotNull GameEventBridge installBridge(@NotNull RapunzelContext context, @NotNull GameEventBus bus);
}
