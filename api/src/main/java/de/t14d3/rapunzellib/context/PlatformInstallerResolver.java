package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

/**
 * Convenience utility for resolving a platform-specific installer via {@link FeatureInstallerRegistry}.
 */
public final class PlatformInstallerResolver {
    private PlatformInstallerResolver() {
    }

    /**
     * Resolves an installer for the given platform using ServiceLoader discovery.
     *
     * @param installerType       the installer type class
     * @param platformId          the target platform
     * @param platformExtractor   function to extract the platform ID from an installer
     * @param moduleHintPrefix    prefix for the dependency error hint
     * @param <T>                 the installer type
     * @return the resolved installer
     * @throws IllegalStateException if no installer is found
     */
    public static <T> @NotNull T resolve(
        @NotNull Class<T> installerType,
        @NotNull PlatformId platformId,
        @NotNull Function<? super T, PlatformId> platformExtractor,
        @NotNull String moduleHintPrefix
    ) {
        Objects.requireNonNull(installerType, "installerType");
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(platformExtractor, "platformExtractor");
        Objects.requireNonNull(moduleHintPrefix, "moduleHintPrefix");

        return FeatureInstallerRegistry.create(installerType, platformExtractor, moduleHintPrefix).resolve(platformId);
    }
}
