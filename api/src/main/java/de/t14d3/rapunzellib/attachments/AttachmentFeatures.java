package de.t14d3.rapunzellib.attachments;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Entry point for installing and accessing attachment features on a platform.
 *
 * <p>Resolves the platform-specific {@link AttachmentFeatureInstaller} via {@link ServiceLoader}
 * and provides static convenience methods for attachment operations.</p>
 */
public final class AttachmentFeatures {
    private static final FeatureInstallerRegistry<AttachmentFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        AttachmentFeatureInstaller.class,
        AttachmentFeatureInstaller::platformId,
        platformId -> "Add dependency rapunzellib-platform-" + platformId.name().toLowerCase(Locale.ROOT) +
            " (attachment support is provided by the matching platform module)."
    );

    private AttachmentFeatures() {
    }

    /**
     * Installs attachment support using the current context.
     *
     * @return the attachment support instance
     */
    public static @NotNull AttachmentSupport install() {
        return install(Rapunzel.context());
    }

    /**
     * Installs attachment support into the given context.
     *
     * @param context the context to install into
     * @return the attachment support instance
     */
    public static @NotNull AttachmentSupport install(@NotNull RapunzelContext context) {
        return FeatureInstallationSupport.install(
            context,
            AttachmentSupport.class,
            RuntimeCapability.ATTACHMENTS,
            "attachment features",
            () -> {
                AttachmentFeatureInstaller installer = INSTALLER_REGISTRY.resolve(context.platformId());
                context.getOrCreate(AttachmentSupport.class, installer::support);
                installer.install(context);
            }
        );
    }

    /**
     * Returns the attachment support from the current context or runtime.
     *
     * @return the attachment support
     */
    public static @NotNull AttachmentSupport support() {
        RapunzelContext context = Rapunzel.context();
        return context.services().find(AttachmentSupport.class).orElseGet(() -> support(context.runtime()));
    }

    public static boolean supports(@NotNull RNative target) {
        return install().supports(target);
    }

    public static <T extends RNative> @NotNull T requireSupported(@NotNull T target) {
        return install().requireSupported(target);
    }

    public static @NotNull RAttachmentContainer attachments(@NotNull RNative target) {
        return install().attachments(target);
    }

    public static @NotNull AttachmentSupport support(@NotNull PlatformRuntime runtime) {
        Objects.requireNonNull(runtime, "runtime");
        if (!runtime.hasCapability(RuntimeCapability.ATTACHMENTS)) {
            return AttachmentSupport.empty(runtime.platformId());
        }
        return support(runtime.platformId());
    }

    public static @NotNull AttachmentSupport support(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        return findInstaller(platformId)
            .map(AttachmentFeatureInstaller::support)
            .orElseGet(() -> AttachmentSupport.empty(platformId));
    }

    private static @NotNull Optional<AttachmentFeatureInstaller> findInstaller(@NotNull PlatformId platformId) {
        try {
            return Optional.of(INSTALLER_REGISTRY.resolve(platformId));
        } catch (IllegalStateException ignored) {
            return Optional.empty();
        }
    }
}
