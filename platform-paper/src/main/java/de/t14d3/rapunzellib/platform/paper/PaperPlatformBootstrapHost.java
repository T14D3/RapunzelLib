package de.t14d3.rapunzellib.platform.paper;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BoundPlatformBootstrapHost;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class PaperPlatformBootstrapHost extends BoundPlatformBootstrapHost<JavaPlugin> {
    public static final String PLUGIN_NAME = "RapunzelLib";

    private static final PaperPlatformBootstrapHost INSTANCE = new PaperPlatformBootstrapHost();

    private PaperPlatformBootstrapHost() {
        super(PLUGIN_NAME);
    }

    public static @NotNull PaperPlatformBootstrapHost registerCanonicalHost(@NotNull JavaPlugin plugin) {
        INSTANCE.bindPlugin(plugin);
        Rapunzel.registerPlatformBootstrapHost(INSTANCE);
        return INSTANCE;
    }

    public static void onCanonicalPluginDisable(@NotNull JavaPlugin plugin) {
        INSTANCE.onPluginDisable(plugin);
    }

    public void bindPlugin(@NotNull JavaPlugin plugin) {
        bindOwner(plugin);
    }

    /**
     * Returns the bound Paper platform plugin instance.
     *
     * @return the platform plugin
     * @throws IllegalStateException if no plugin has been bound yet
     */
    public static @NotNull JavaPlugin getPlugin() {
        return INSTANCE.boundOwner()
                .orElseThrow(() -> new IllegalStateException(
                        "PaperPlatformPlugin has not been bound yet. "
                                + "Ensure PaperPlatformPlugin.onLoad() has been called."));
    }

    public void onPluginDisable(@NotNull JavaPlugin plugin) {
        shutdownAndUnbind(plugin);
    }

    @Override
    protected @NotNull String displayName(@NotNull JavaPlugin plugin) {
        return plugin.getName();
    }
}
