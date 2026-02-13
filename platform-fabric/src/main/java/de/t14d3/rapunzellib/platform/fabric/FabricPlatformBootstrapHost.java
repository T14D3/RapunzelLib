package de.t14d3.rapunzellib.platform.fabric;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BoundPlatformBootstrapHost;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FabricPlatformBootstrapHost extends BoundPlatformBootstrapHost<MinecraftServer> {
    public static final String MOD_ID = "rapunzellib_platform_fabric";

    private static final FabricPlatformBootstrapHost INSTANCE = new FabricPlatformBootstrapHost();

    private FabricPlatformBootstrapHost() {
        super(MOD_ID);
    }

    public static @NotNull FabricPlatformBootstrapHost registerCanonicalHost() {
        Rapunzel.registerPlatformBootstrapHost(INSTANCE);
        return INSTANCE;
    }

    public void bindServer(@NotNull MinecraftServer server) {
        bindOwner(server);
    }

    public void onServerStopping(@NotNull MinecraftServer server) {
        shutdownAndUnbind(server);
    }

    public void onServerStopped(@NotNull MinecraftServer server) {
        unbind(server);
    }

    @Override
    protected @NotNull String displayName(@NotNull MinecraftServer server) {
        return MOD_ID;
    }
}
