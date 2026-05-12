package de.t14d3.rapunzellib.objects;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Abstract base implementation of {@link RNative} that stores a typed native handle.
 *
 * <p>The handle field is volatile to allow safe publication in scenarios where the
 * underlying native object may be replaced (e.g. respawning).</p>
 *
 * @param <H> the native handle type
 */
public abstract class RNativeHandle<H> extends RNativeBase {
    private volatile H handle;

    /**
     * Creates a native handle with the given platform ID and handle.
     *
     * @param platformId the platform identifier
     * @param handle     the native handle
     */
    protected RNativeHandle(@NotNull PlatformId platformId, @NotNull H handle) {
        super(platformId);
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    /**
     * Creates a native handle with the given platform ID, handle, and attachment container.
     *
     * @param platformId  the platform identifier
     * @param handle      the native handle
     * @param attachments the attachment container to use
     */
    protected RNativeHandle(@NotNull PlatformId platformId, @NotNull H handle, @NotNull RAttachmentContainer attachments) {
        super(platformId, attachments);
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    @Override
    public @NotNull H handle() {
        return handle;
    }

    /**
     * Updates the native handle reference.
     *
     * @param newHandle the new native handle
     */
    public final void updateNativeHandle(@NotNull H newHandle) {
        this.handle = Objects.requireNonNull(newHandle, "newHandle");
    }
}
