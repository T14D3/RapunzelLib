package de.t14d3.rapunzellib.platform.sponge;

import com.google.inject.Inject;
import org.spongepowered.api.Server;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.nio.file.Path;

@Plugin(SpongePlatformBootstrapHost.PLUGIN_ID)
public final class SpongePlatformPlugin {
    private final PluginContainer container;
    private final Path dataDirectory;

    @Inject
    public SpongePlatformPlugin(PluginContainer container, @ConfigDir(sharedRoot = false) Path dataDirectory) {
        this.container = container;
        this.dataDirectory = dataDirectory;
    }

    @Listener
    public void onServerStarted(StartedEngineEvent<Server> event) {
        SpongeRapunzelBootstrap.bootstrapPlatform(container, dataDirectory, event.engine());
    }

    @Listener
    public void onServerStopping(StoppingEngineEvent<Server> event) {
        SpongePlatformBootstrapHost.onCanonicalPluginStopping(container);
    }
}
