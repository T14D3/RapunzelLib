package de.t14d3.rapunzellib.platform.fabric;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.platform.PlatformFeatureInstaller;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricBlocks;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricEntities;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricNativeInteropSupport;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricPlayers;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricWrapperStore;
import de.t14d3.rapunzellib.platform.fabric.entity.FabricWorlds;
import de.t14d3.rapunzellib.platform.shared.attachments.SharedAttachmentService;
import de.t14d3.rapunzellib.platform.shared.registry.SharedRegistryBridge;
import de.t14d3.rapunzellib.objects.WrapperStore;
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
        SharedAttachmentService attachmentService = context.services().get(SharedAttachmentService.class);
        FabricWorlds worlds = new FabricWorlds(attachmentService, server);
        FabricPlayers players = new FabricPlayers(attachmentService, server, worlds);
        FabricEntities entities = new FabricEntities(attachmentService, server, players, worlds);
        FabricBlocks blocks = new FabricBlocks(attachmentService, worlds);
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
        context.services().register(WrapperStore.class, new FabricWrapperStore());
    }
}
