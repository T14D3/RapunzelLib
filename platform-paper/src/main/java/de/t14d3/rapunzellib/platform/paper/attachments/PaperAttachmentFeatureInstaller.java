package de.t14d3.rapunzellib.platform.paper.attachments;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.AttachmentFeatureInstaller;
import de.t14d3.rapunzellib.attachments.AttachmentFeatureInstallerSupport;
import de.t14d3.rapunzellib.attachments.AttachmentSupport;
import de.t14d3.rapunzellib.attachments.AttachmentTargetType;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

public final class PaperAttachmentFeatureInstaller implements AttachmentFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public @NotNull AttachmentSupport support() {
        return AttachmentFeatureInstallerSupport.serverSupportBuilder(platformId())
            .persistent(
                AttachmentTargetType.PLAYER,
                AttachmentTargetType.ENTITY,
                AttachmentTargetType.WORLD
            )
            .optionalPersistent(AttachmentTargetType.BLOCK)
            .build();
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
    }
}
