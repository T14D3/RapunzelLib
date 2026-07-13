package de.t14d3.rapunzellib.livetest;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

/**
 * Platform-specific installer for live test features.
 * <p>
 * Each platform (Paper, Fabric, NeoForge, Sponge, etc.) provides an implementation
 * that registers its {@link LiveTestHost}, {@link BotService}, and command
 * infrastructure for that platform's specific server environment.
 * </p>
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader} and registered
 * in a {@link de.t14d3.rapunzellib.context.FeatureInstallerRegistry}.
 * </p>
 */
public interface LiveTestFeatureInstaller {

    /**
     * Returns the platform this installer supports.
     *
     * @return the platform identifier
     */
    @NotNull PlatformId platformId();

    /**
     * Installs live test features into the given Rapunzel context.
     * <p>
     * Implementations should register at minimum a {@link LiveTestHost} and
     * a {@link BotService} into the context's {@link de.t14d3.rapunzellib.context.ServiceRegistry}.
     * They should also register the {@code /livetest} command using the platform's
     * command registration system.
     * </p>
     *
     * @param context the Rapunzel context to install into
     */
    void install(@NotNull RapunzelContext context);
}
