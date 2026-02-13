package de.t14d3.rapunzellib.platform.paper;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.bootstrap.BoundPlatformBootstrapHost;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PaperPlatformBootstrapHost extends BoundPlatformBootstrapHost<JavaPlugin> {
    public static final String PLUGIN_NAME = "RapunzelLibPlatformPaper";

    private static final PaperPlatformBootstrapHost INSTANCE = new PaperPlatformBootstrapHost();

    private PaperPlatformBootstrapHost() {
        super(PLUGIN_NAME);
    }

    public static @NotNull PaperPlatformBootstrapHost prepareBootstrap(@NotNull JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (plugin instanceof PaperPlatformPlugin) {
            return registerCanonicalHost(plugin);
        }
        return INSTANCE;
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

    public void onPluginDisable(@NotNull JavaPlugin plugin) {
        shutdownAndUnbind(plugin);
    }

    @Override
    protected @NotNull String displayName(@NotNull JavaPlugin plugin) {
        return plugin.getName();
    }
}
