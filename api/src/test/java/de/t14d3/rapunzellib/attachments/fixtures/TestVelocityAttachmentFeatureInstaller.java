package de.t14d3.rapunzellib.attachments.fixtures;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.AttachmentFeatureInstaller;
import de.t14d3.rapunzellib.attachments.AttachmentSupport;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

public final class TestVelocityAttachmentFeatureInstaller implements AttachmentFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.VELOCITY;
    }

    @Override
    public @NotNull AttachmentSupport support() {
        return TestAttachmentFeatureInstallers.velocitySupport();
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        TestAttachmentFeatureInstallers.installVelocity(context);
    }
}
