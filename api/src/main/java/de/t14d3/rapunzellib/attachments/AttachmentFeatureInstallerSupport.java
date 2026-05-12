package de.t14d3.rapunzellib.attachments;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Pre-configured builder helpers for creating {@link AttachmentSupport} instances
 * for server and proxy platforms.
 */
public final class AttachmentFeatureInstallerSupport {
    private static final AttachmentTargetType[] SERVER_TARGET_TYPES = {
        AttachmentTargetType.PLAYER,
        AttachmentTargetType.ENTITY,
        AttachmentTargetType.WORLD,
        AttachmentTargetType.BLOCK,
    };
    private static final AttachmentTargetType[] PROXY_TARGET_TYPES = {
        AttachmentTargetType.PLAYER,
    };

    private AttachmentFeatureInstallerSupport() {
    }

    /**
     * Creates a support builder pre-configured for server platforms with persistent items.
     *
     * @param platformId the platform ID
     * @return the support builder
     */
    public static @NotNull AttachmentSupport.Builder serverSupportBuilder(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        return AttachmentSupport.builder(platformId)
            .persistentItems();
    }

    /**
     * Creates a server support instance with transient-only storage for all target types.
     *
     * @param platformId the platform ID
     * @return the attachment support
     */
    public static @NotNull AttachmentSupport serverTransientSupport(@NotNull PlatformId platformId) {
        return serverSupportBuilder(platformId)
            .transientOnly(SERVER_TARGET_TYPES)
            .build();
    }

    /**
     * Creates a server support instance with persistent storage for all target types.
     *
     * @param platformId the platform ID
     * @return the attachment support
     */
    public static @NotNull AttachmentSupport serverPersistentSupport(@NotNull PlatformId platformId) {
        return serverSupportBuilder(platformId)
            .persistent(SERVER_TARGET_TYPES)
            .build();
    }

    /**
     * Creates a support builder pre-configured for proxy platforms with persistent items.
     *
     * @param platformId the platform ID
     * @return the support builder
     */
    public static @NotNull AttachmentSupport.Builder proxySupportBuilder(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        return AttachmentSupport.builder(platformId)
            .persistentItems();
    }

    /**
     * Creates a proxy support instance with persistent storage for player targets.
     *
     * @param platformId the platform ID
     * @return the attachment support
     */
    public static @NotNull AttachmentSupport proxyPersistentPlayerSupport(@NotNull PlatformId platformId) {
        return proxySupportBuilder(platformId)
            .persistent(PROXY_TARGET_TYPES)
            .build();
    }
}
