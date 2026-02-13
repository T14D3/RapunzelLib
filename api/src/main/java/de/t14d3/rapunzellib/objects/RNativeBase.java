package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class RNativeBase implements RNative {
    private final PlatformId platformId;
    private final RAttachmentContainer attachments;

    protected RNativeBase(@NotNull PlatformId platformId) {
        this(platformId, RAttachmentContainer.lazyMutable());
    }

    protected RNativeBase(@NotNull PlatformId platformId, @NotNull RAttachmentContainer attachments) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        this.attachments = Objects.requireNonNull(attachments, "attachments");
    }

    @Override
    public final @NotNull PlatformId platformId() {
        return platformId;
    }

    @Override
    public final @NotNull RAttachmentContainer attachments() {
        return attachments;
    }
}
