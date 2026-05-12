package de.t14d3.rapunzellib.inventory;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

/**
 * A platform-specific installer that registers inventory wrapper factories
 * and any other inventory-related bindings into a {@link RapunzelContext}.
 * <p>
 * Each platform module supplies its own implementation which is resolved
 * at runtime through the {@link InventoryFeatures} entry point.
 */
public interface InventoryFeatureInstaller {

    /**
     * Returns the platform this installer is associated with.
     *
     * @return the platform identifier
     */
    @NotNull PlatformId platformId();

    /**
     * Installs inventory features into the given context, typically by calling
     * {@link InventoryFeatureInstallerSupport#registerInventories} with the
     * platform's wrapper factories.
     *
     * @param context the Rapunzel context to install into
     */
    void install(@NotNull RapunzelContext context);
}
