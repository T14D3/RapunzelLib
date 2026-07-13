package de.t14d3.rapunzellib.platform.paper;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.commands.paper.PaperCommandFeatureInstaller;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper platform bootstrap plugin for RapunzelLib.
 *
 * <p>This is the entry point discovered by Paper's plugin loader.
 * It registers the canonical bootstrap host and owns the shared
 * RapunzelLib context that consumer plugins borrow via
 * {@link PaperRapunzelBootstrap#acquire}.</p>
 */
public final class PaperPlatformPlugin extends JavaPlugin {
    @Override
    public void onLoad() {
        PaperPlatformBootstrapHost.registerCanonicalHost(this);
        // Paper lifecycle command handler MUST be registered in onLoad()
        // (lifecycle state is NONE). The actual command services are wired
        // during onEnable() and picked up by the lifecycle handler when
        // the COMMANDS lifecycle event fires.
        PaperCommandFeatureInstaller.registerLifecycleHandler(this);
    }

    @Override
    public void onEnable() {
        // Bootstrap the shared platform context with all service registrations
        PaperRapunzelBootstrap.bootstrapPlatform(this);
    }

    @Override
    public void onDisable() {
        // Shut down the platform context (consumers have already been shut down)
        Rapunzel.shutdown(this);
    }
}
