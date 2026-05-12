package de.t14d3.rapunzellib.platform;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

/**
 * Platform-specific installer that registers platform services into a RapunzelLib context.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.</p>
 */
public interface PlatformFeatureInstaller {
    /**
     * Returns the platform this installer targets.
     *
     * @return the platform ID
     */
    @NotNull PlatformId platformId();

    /**
     * Installs platform services into the given context.
     *
     * @param context the context to install into
     */
    void install(@NotNull RapunzelContext context);
}
