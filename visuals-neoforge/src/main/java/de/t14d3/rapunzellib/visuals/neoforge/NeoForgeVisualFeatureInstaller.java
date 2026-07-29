package de.t14d3.rapunzellib.visuals.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.visuals.VisualFeatureInstaller;
import de.t14d3.rapunzellib.visuals.Visuals;
import org.jetbrains.annotations.NotNull;

public final class NeoForgeVisualFeatureInstaller implements VisualFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        NeoForgeVisuals visuals = new NeoForgeVisuals(context);
        context.services().find(GameEventBus.class).ifPresent(bus ->
            bus.onPost(PlayerQuitPost.class, e -> visuals.manager().cleanupForPlayer(e.uuid()))
        );
        context.register(Visuals.class, visuals);
    }
}
