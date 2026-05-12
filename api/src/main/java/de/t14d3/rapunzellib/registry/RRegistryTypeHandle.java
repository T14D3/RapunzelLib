package de.t14d3.rapunzellib.registry;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Abstract base implementation of {@link RRegistryType} backed by a typed native handle.
 *
 * @param <H> the native handle type
 */
public abstract class RRegistryTypeHandle<H> extends RNativeHandle<H> implements RRegistryType {
    private final RKey key;

    /**
     * Creates a registry type handle with the given platform, key, and native handle.
     *
     * @param platformId the platform identifier
     * @param key        the type key
     * @param handle     the native handle
     */
    protected RRegistryTypeHandle(@NotNull PlatformId platformId, @NotNull RKey key, @NotNull H handle) {
        super(platformId, handle);
        this.key = Objects.requireNonNull(key, "key");
    }

    /**
     * Creates a registry type handle with the given platform, key, native handle, and attachment container.
     *
     * @param platformId  the platform identifier
     * @param key         the type key
     * @param handle      the native handle
     * @param attachments the attachment container to use
     */
    protected RRegistryTypeHandle(@NotNull PlatformId platformId, @NotNull RKey key, @NotNull H handle, @NotNull RAttachmentContainer attachments) {
        super(platformId, handle, attachments);
        this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    public final @NotNull RKey key() {
        return key;
    }
}
