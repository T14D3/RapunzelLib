package de.t14d3.rapunzellib.platform.sponge;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.platform.PlatformFeatureInstaller;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import de.t14d3.rapunzellib.platform.sponge.objects.SpongeBlocks;
import de.t14d3.rapunzellib.platform.sponge.objects.SpongeEntities;
import de.t14d3.rapunzellib.platform.sponge.objects.SpongeNativeInteropSupport;
import de.t14d3.rapunzellib.platform.sponge.objects.SpongePlayers;
import de.t14d3.rapunzellib.platform.sponge.objects.SpongeWrapperStore;
import de.t14d3.rapunzellib.platform.sponge.objects.SpongeWorlds;
import de.t14d3.rapunzellib.platform.sponge.registry.SpongeRegistryBridge;
import de.t14d3.rapunzellib.objects.WrapperStore;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Server;

public final class SpongePlatformFeatureInstaller implements PlatformFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.SPONGE;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        Server server = context.services().get(Server.class);
        SpongeAttachmentService attachmentService = context.services().get(SpongeAttachmentService.class);
        SpongeWorlds worlds = new SpongeWorlds(attachmentService);
        SpongePlayers players = new SpongePlayers(attachmentService, worlds);
        SpongeEntities entities = new SpongeEntities(players, attachmentService, worlds);
        SpongeBlocks blocks = new SpongeBlocks(attachmentService, worlds);
        BootstrapServices.registerServerPlatformServices(
            context,
            players,
            SpongePlayers.class,
            entities,
            SpongeEntities.class,
            worlds,
            SpongeWorlds.class,
            blocks,
            SpongeBlocks.class,
            SpongeNativeInteropSupport::register,
            () -> SpongeRegistryBridge.createRegistryAccess(server)
        );
        context.services().register(WrapperStore.class, new SpongeWrapperStore());
    }
}
