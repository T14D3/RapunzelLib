package de.t14d3.rapunzellib.platform.velocity;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BoundPlatformBootstrapHost;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class VelocityPlatformBootstrapHost extends BoundPlatformBootstrapHost<Object> {
    public static final String PLUGIN_ID = "rapunzellib_platform_velocity";

    private static final VelocityPlatformBootstrapHost INSTANCE = new VelocityPlatformBootstrapHost();

    private VelocityPlatformBootstrapHost() {
        super(PLUGIN_ID);
    }

    public static @NotNull VelocityPlatformBootstrapHost prepareBootstrap(@NotNull Object plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (plugin instanceof VelocityPlatformPlugin) {
            return registerCanonicalHost(plugin);
        }
        return INSTANCE;
    }

    public static @NotNull VelocityPlatformBootstrapHost registerCanonicalHost(@NotNull Object plugin) {
        INSTANCE.bindPlugin(plugin);
        Rapunzel.registerPlatformBootstrapHost(INSTANCE);
        return INSTANCE;
    }

    public static void onCanonicalPluginShutdown(@NotNull Object plugin) {
        INSTANCE.onProxyShutdown(plugin);
    }

    public void bindPlugin(@NotNull Object plugin) {
        bindOwner(plugin);
    }

    public void onProxyShutdown(@NotNull Object plugin) {
        shutdownAndUnbind(plugin);
    }

    @Override
    protected @NotNull String displayName(@NotNull Object plugin) {
        return plugin.getClass().getName();
    }
}
