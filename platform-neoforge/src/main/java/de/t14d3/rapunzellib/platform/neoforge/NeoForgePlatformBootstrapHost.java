package de.t14d3.rapunzellib.platform.neoforge;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BoundPlatformBootstrapHost;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class NeoForgePlatformBootstrapHost extends BoundPlatformBootstrapHost<MinecraftServer> {
    private static final NeoForgePlatformBootstrapHost INSTANCE = new NeoForgePlatformBootstrapHost();

    private NeoForgePlatformBootstrapHost() {
        super(NeoForgeRapunzelBootstrap.MOD_ID);
    }

    public static @NotNull NeoForgePlatformBootstrapHost registerCanonicalHost() {
        Rapunzel.registerPlatformBootstrapHost(INSTANCE);
        return INSTANCE;
    }

    public void bindServer(@NotNull MinecraftServer server) {
        bindOwner(server);
    }

    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        bindServer(event.getServer());
    }

    public void onServerStopping(ServerStoppingEvent event) {
        shutdownAndUnbind(event.getServer());
    }

    public void onServerStopped(ServerStoppedEvent event) {
        unbind(event.getServer());
    }

    @Override
    protected @NotNull String displayName(@NotNull MinecraftServer server) {
        return NeoForgeRapunzelBootstrap.MOD_ID;
    }
}
