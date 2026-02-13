package de.t14d3.rapunzellib.gui.paper;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.gui.GuiFeatureInstallerSupport;
import de.t14d3.rapunzellib.gui.paper.inventory.InventoryClickListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Provider for Paper platform GUI functionality.
 * Must be registered during plugin enable with a valid JavaPlugin instance.
 */
public final class PaperGuiProvider {

    private static JavaPlugin plugin;
    private static InventoryClickListener inventoryClickListener;

    private PaperGuiProvider() {
    }

    /**
     * Registers the Paper GUI feature installer into the provided context.
     */
    public static void register(@NotNull RapunzelContext ctx) {
        GuiFeatureInstallerSupport.install(ctx, new PaperGuiFeatureInstaller());
    }


    static void setInstalledState(@NotNull JavaPlugin plugin, @NotNull InventoryClickListener listener) {
        PaperGuiProvider.plugin = plugin;
        inventoryClickListener = listener;
    }

    /**
     * Get the plugin instance.
     * @return The JavaPlugin instance, or null if not initialized
     */
    public static JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Check if the provider has been initialized.
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return plugin != null;
    }
}
