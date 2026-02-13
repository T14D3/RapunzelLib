package de.t14d3.rapunzellib.platform.paper;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.platform.PlatformFeatureInstaller;
import de.t14d3.rapunzellib.platform.shared.registry.SharedRegistryBridge;
import de.t14d3.rapunzellib.platform.paper.objects.PaperBlocks;
import de.t14d3.rapunzellib.platform.paper.objects.PaperEntities;
import de.t14d3.rapunzellib.platform.paper.objects.PaperNativeInteropSupport;
import de.t14d3.rapunzellib.platform.paper.objects.PaperPlayers;
import de.t14d3.rapunzellib.platform.paper.objects.PaperWorlds;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public final class PaperPlatformFeatureInstaller implements PlatformFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        MinecraftServer server = context.services().get(MinecraftServer.class);
        PaperWorlds worlds = new PaperWorlds(server);
        PaperPlayers players = new PaperPlayers(server, worlds);
        PaperEntities entities = new PaperEntities(server, players, worlds);
        PaperBlocks blocks = new PaperBlocks(worlds);
        BootstrapServices.registerServerPlatformServices(
            context,
            players,
            PaperPlayers.class,
            entities,
            PaperEntities.class,
            worlds,
            PaperWorlds.class,
            blocks,
            PaperBlocks.class,
            PaperNativeInteropSupport::register,
            () -> SharedRegistryBridge.createRegistryAccess(platformId())
        );
    }
}
