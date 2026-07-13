package de.t14d3.rapunzellib.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

public final class VelocityPlatformPlugin {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public VelocityPlatformPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        VelocityRapunzelBootstrap.bootstrapPlatform(this, proxy, logger, dataDirectory);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        VelocityPlatformBootstrapHost.onCanonicalPluginShutdown(this);
    }
}
