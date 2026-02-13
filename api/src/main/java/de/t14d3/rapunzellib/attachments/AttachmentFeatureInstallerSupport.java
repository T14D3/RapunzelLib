package de.t14d3.rapunzellib.attachments;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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

    public static @NotNull AttachmentSupport.Builder serverSupportBuilder(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        return AttachmentSupport.builder(platformId)
            .persistentItems();
    }

    public static @NotNull AttachmentSupport serverTransientSupport(@NotNull PlatformId platformId) {
        return serverSupportBuilder(platformId)
            .transientOnly(SERVER_TARGET_TYPES)
            .build();
    }

    public static @NotNull AttachmentSupport serverPersistentSupport(@NotNull PlatformId platformId) {
        return serverSupportBuilder(platformId)
            .persistent(SERVER_TARGET_TYPES)
            .build();
    }

    public static @NotNull AttachmentSupport.Builder proxySupportBuilder(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        return AttachmentSupport.builder(platformId)
            .persistentItems();
    }

    public static @NotNull AttachmentSupport proxyPersistentPlayerSupport(@NotNull PlatformId platformId) {
        return proxySupportBuilder(platformId)
            .persistent(PROXY_TARGET_TYPES)
            .build();
    }
}
