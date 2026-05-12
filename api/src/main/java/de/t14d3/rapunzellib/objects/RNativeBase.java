package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Abstract base implementation of {@link RNative} with platform ID and attachment container storage.
 */
public abstract class RNativeBase implements RNative {
    private final PlatformId platformId;
    private final RAttachmentContainer attachments;

    /**
     * Creates a native base with the given platform ID and a lazy-mutable attachment container.
     *
     * @param platformId the platform identifier
     */
    protected RNativeBase(@NotNull PlatformId platformId) {
        this(platformId, RAttachmentContainer.lazyMutable());
    }

    /**
     * Creates a native base with the given platform ID and attachment container.
     *
     * @param platformId  the platform identifier
     * @param attachments the attachment container to use
     */
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
