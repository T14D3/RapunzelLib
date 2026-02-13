package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public final class PlatformInstallerResolver {
    private PlatformInstallerResolver() {
    }

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
