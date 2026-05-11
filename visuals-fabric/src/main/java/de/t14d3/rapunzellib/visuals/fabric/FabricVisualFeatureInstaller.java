package de.t14d3.rapunzellib.visuals.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.visuals.VisualFeatureInstaller;
import de.t14d3.rapunzellib.visuals.Visuals;
import org.jetbrains.annotations.NotNull;

public final class FabricVisualFeatureInstaller implements VisualFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.FABRIC;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        FabricVisuals visuals = new FabricVisuals(context);
        context.services().find(GameEventBus.class).ifPresent(bus ->
            bus.onPost(PlayerQuitPost.class, e -> ((FabricVisualManager) visuals.manager()).cleanupForPlayer(e.uuid()))
        );
        context.register(Visuals.class, visuals);
    }
}
