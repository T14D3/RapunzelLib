package de.t14d3.rapunzellib.platform.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.common.bootstrap.BootstrapServices;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.objects.interop.MutableRNativeInterop;
import de.t14d3.rapunzellib.platform.PlatformFeatureInstaller;
import de.t14d3.rapunzellib.platform.velocity.objects.VelocityNativeInteropSupport;
import de.t14d3.rapunzellib.platform.velocity.objects.VelocityPersistentAttachmentsStore;
import de.t14d3.rapunzellib.platform.velocity.objects.VelocityPlayers;
import org.jetbrains.annotations.NotNull;

public final class VelocityPlatformFeatureInstaller implements PlatformFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.VELOCITY;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        ProxyServer proxy = context.services().get(ProxyServer.class);
        VelocityPersistentAttachmentsStore persistentAttachmentsStore =
            context.services().get(VelocityPersistentAttachmentsStore.class);
        VelocityPlayers players = new VelocityPlayers(proxy, persistentAttachmentsStore);
        BootstrapServices.registerPlayerAccessors(context, players, VelocityPlayers.class);
        VelocityNativeInteropSupport.register(context.services().get(MutableRNativeInterop.class));
    }
}
