package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

/**
 * Platform-specific installer for command features.
 * <p>
 * Each platform (Bukkit, Sponge, etc.) provides an implementation that
 * registers its command source adapters and configures the command service
 * for that platform's specific command framework integration.
 * </p>
 */
public interface CommandFeatureInstaller {
    /**
     * Gets the platform identifier for this installer.
     *
     * @return the platform ID
     */
    @NotNull PlatformId platformId();

    /**
     * Installs command features into the given Rapunzel context.
     *
     * @param context the Rapunzel context to install into
     */
    void install(@NotNull RapunzelContext context);
}
