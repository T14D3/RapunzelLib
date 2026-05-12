package de.t14d3.rapunzellib.nbt;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for platform-specific NBT feature installers.
 * <p>
 * Each platform registers an implementation via the {@link de.t14d3.rapunzellib.context.FeatureInstallerRegistry}
 * to provide its native NBT serialization and item stack adapter services.</p>
 */
public interface NbtFeatureInstaller {
    /**
     * Returns the platform ID this installer targets.
     *
     * @return the platform ID
     */
    @NotNull PlatformId platformId();

    /**
     * Installs NBT feature services (serializers, adapters) into the given context.
     *
     * @param context the Rapunzel context to install into
     */
    void install(@NotNull RapunzelContext context);
}
