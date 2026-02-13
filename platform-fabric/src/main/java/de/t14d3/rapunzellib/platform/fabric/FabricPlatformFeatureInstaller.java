package de.t14d3.rapunzellib.platform.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.platform.PlatformFeatureInstaller;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricBlocks;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricEntities;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricNativeInteropSupport;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricPlayers;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricWorlds;
import de.t14d3.rapunzellib.platform.shared.registry.SharedRegistryBridge;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public final class FabricPlatformFeatureInstaller implements PlatformFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.FABRIC;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        MinecraftServer server = context.services().get(MinecraftServer.class);
        FabricWorlds worlds = new FabricWorlds(server);
        FabricPlayers players = new FabricPlayers(server, worlds);
        FabricEntities entities = new FabricEntities(server, players, worlds);
        FabricBlocks blocks = new FabricBlocks(worlds);
        BootstrapServices.registerServerPlatformServices(
            context,
            players,
            FabricPlayers.class,
            entities,
            FabricEntities.class,
            worlds,
            FabricWorlds.class,
            blocks,
            FabricBlocks.class,
            FabricNativeInteropSupport::register,
            () -> SharedRegistryBridge.createRegistryAccess(platformId())
        );
    }
}
