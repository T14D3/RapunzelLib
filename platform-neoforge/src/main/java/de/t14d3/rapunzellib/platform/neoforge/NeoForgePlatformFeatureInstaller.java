package de.t14d3.rapunzellib.platform.neoforge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.platform.PlatformFeatureInstaller;
import de.t14d3.rapunzellib.platform.shared.registry.SharedRegistryBridge;
import de.t14d3.rapunzellib.platform.neoforge.entity.NeoForgeBlocks;
import de.t14d3.rapunzellib.platform.neoforge.entity.NeoForgeEntities;
import de.t14d3.rapunzellib.platform.neoforge.entity.NeoForgeNativeInteropSupport;
import de.t14d3.rapunzellib.platform.neoforge.entity.NeoForgePlayers;
import de.t14d3.rapunzellib.platform.neoforge.entity.NeoForgeWorlds;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public final class NeoForgePlatformFeatureInstaller implements PlatformFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        MinecraftServer server = context.services().get(MinecraftServer.class);
        NeoForgeWorlds worlds = new NeoForgeWorlds(server);
        NeoForgePlayers players = new NeoForgePlayers(server, worlds);
        NeoForgeEntities entities = new NeoForgeEntities(server, players, worlds);
        NeoForgeBlocks blocks = new NeoForgeBlocks(worlds);
        BootstrapServices.registerServerPlatformServices(
            context,
            players,
            NeoForgePlayers.class,
            entities,
            NeoForgeEntities.class,
            worlds,
            NeoForgeWorlds.class,
            blocks,
            NeoForgeBlocks.class,
            NeoForgeNativeInteropSupport::register,
            () -> SharedRegistryBridge.createRegistryAccess(platformId())
        );
    }
}
