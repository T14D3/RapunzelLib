package de.t14d3.rapunzellib.platform.fabric.attachments;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.AttachmentFeatureInstaller;
import de.t14d3.rapunzellib.attachments.AttachmentFeatureInstallerSupport;
import de.t14d3.rapunzellib.attachments.AttachmentSupport;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

public final class FabricAttachmentFeatureInstaller implements AttachmentFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.FABRIC;
    }

    @Override
    public @NotNull AttachmentSupport support() {
        return AttachmentFeatureInstallerSupport.serverTransientSupport(platformId());
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
    }
}
