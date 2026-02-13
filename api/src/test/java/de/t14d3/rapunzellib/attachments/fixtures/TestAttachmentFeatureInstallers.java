package de.t14d3.rapunzellib.attachments.fixtures;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.AttachmentSupport;
import de.t14d3.rapunzellib.attachments.AttachmentFeatureInstallerSupport;
import de.t14d3.rapunzellib.attachments.AttachmentTargetType;
import de.t14d3.rapunzellib.context.RapunzelContext;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestAttachmentFeatureInstallers {
    private static final AtomicInteger PAPER_INSTALL_CALLS = new AtomicInteger();
    private static final AtomicInteger VELOCITY_INSTALL_CALLS = new AtomicInteger();

    private TestAttachmentFeatureInstallers() {
    }

    public static void reset() {
        PAPER_INSTALL_CALLS.set(0);
        VELOCITY_INSTALL_CALLS.set(0);
    }

    public static int paperInstallCalls() {
        return PAPER_INSTALL_CALLS.get();
    }

    public static int velocityInstallCalls() {
        return VELOCITY_INSTALL_CALLS.get();
    }

    public static AttachmentSupport paperSupport() {
        return AttachmentFeatureInstallerSupport.serverSupportBuilder(PlatformId.PAPER)
            .persistent(
                AttachmentTargetType.PLAYER,
                AttachmentTargetType.ENTITY,
                AttachmentTargetType.WORLD
            )
            .optionalPersistent(AttachmentTargetType.BLOCK)
            .build();
    }

    public static AttachmentSupport velocitySupport() {
        return AttachmentFeatureInstallerSupport.proxyPersistentPlayerSupport(PlatformId.VELOCITY);
    }

    public static void installPaper(RapunzelContext context) {
        PAPER_INSTALL_CALLS.incrementAndGet();
    }

    public static void installVelocity(RapunzelContext context) {
        VELOCITY_INSTALL_CALLS.incrementAndGet();
    }
}
