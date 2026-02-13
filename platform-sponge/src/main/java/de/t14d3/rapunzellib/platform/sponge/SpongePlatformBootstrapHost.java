package de.t14d3.rapunzellib.platform.sponge;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BoundPlatformBootstrapHost;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.plugin.PluginContainer;

import java.util.Objects;

public final class SpongePlatformBootstrapHost extends BoundPlatformBootstrapHost<PluginContainer> {
    public static final String PLUGIN_ID = "rapunzellib_platform_sponge";

    private static final SpongePlatformBootstrapHost INSTANCE = new SpongePlatformBootstrapHost();

    private SpongePlatformBootstrapHost() {
        super(PLUGIN_ID);
    }

    public static @NotNull SpongePlatformBootstrapHost prepareBootstrap(@NotNull PluginContainer container) {
        Objects.requireNonNull(container, "container");
        if (container.instance() instanceof SpongePlatformPlugin) {
            return registerCanonicalHost(container);
        }
        return INSTANCE;
    }

    public static @NotNull SpongePlatformBootstrapHost registerCanonicalHost(@NotNull PluginContainer container) {
        INSTANCE.bindContainer(container);
        Rapunzel.registerPlatformBootstrapHost(INSTANCE);
        return INSTANCE;
    }

    public static void onCanonicalPluginStopping(@NotNull PluginContainer container) {
        INSTANCE.onEngineStopping(container);
    }

    public void bindContainer(@NotNull PluginContainer container) {
        bindOwner(container);
    }

    public void onEngineStopping(@NotNull PluginContainer container) {
        shutdownAndUnbind(container);
    }

    @Override
    protected @NotNull String displayName(@NotNull PluginContainer container) {
        return container.metadata().id();
    }
}
