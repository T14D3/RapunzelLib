package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

/**
 * Service interface for platform-specific visual feature installers.
 * <p>
 * Each platform (e.g. Paper, Sponge, Minestom) provides an implementation
 * that registers the platform's visual manager and related components.
 */
public interface VisualFeatureInstaller {

    /**
     * Returns the platform ID this installer supports.
     *
     * @return the platform identifier
     */
    @NotNull PlatformId platformId();

    /**
     * Installs the visual features on the given platform context.
     *
     * @param context the Rapunzel context to install into
     */
    void install(@NotNull RapunzelContext context);
}
