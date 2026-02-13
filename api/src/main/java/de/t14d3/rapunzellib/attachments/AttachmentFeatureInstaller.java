package de.t14d3.rapunzellib.attachments;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

public interface AttachmentFeatureInstaller {
    @NotNull PlatformId platformId();

    default @NotNull AttachmentSupport support() {
        return AttachmentSupport.empty(platformId());
    }

    void install(@NotNull RapunzelContext context);
}
