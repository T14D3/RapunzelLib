package de.t14d3.rapunzellib.attachments;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

/**
 * Installs platform-specific attachment features during bootstrap.
 */
public interface AttachmentFeatureInstaller {
    /**
     * Returns the platform this installer targets.
     *
     * @return the platform identifier
     */
    @NotNull PlatformId platformId();

    /**
     * Provides attachment support for this platform.
     *
     * @return attachment support instance, never null
     */
    default @NotNull AttachmentSupport support() {
        return AttachmentSupport.empty(platformId());
    }

    /**
     * Installs attachment features into the given context.
     *
     * @param context the Rapunzel context to install into
     */
    void install(@NotNull RapunzelContext context);
}
