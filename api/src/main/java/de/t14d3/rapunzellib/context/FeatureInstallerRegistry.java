package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A registry that discovers platform-specific installers via {@link ServiceLoader}.
 *
 * @param <T> the installer type
 */
public final class FeatureInstallerRegistry<T> {
    private final Class<T> installerType;
    private final Function<? super PlatformId, String> dependencyHintFactory;
    private final Map<PlatformId, T> installersByPlatform;

    private FeatureInstallerRegistry(
        @NotNull Class<T> installerType,
        @NotNull Function<? super T, PlatformId> platformExtractor,
        @NotNull Function<? super PlatformId, String> dependencyHintFactory
    ) {
        this.installerType = Objects.requireNonNull(installerType, "installerType");
        Objects.requireNonNull(platformExtractor, "platformExtractor");
        this.dependencyHintFactory = Objects.requireNonNull(dependencyHintFactory, "dependencyHintFactory");

        Map<PlatformId, T> resolvedInstallers = new EnumMap<>(PlatformId.class);
        ServiceLoader.load(installerType, installerType.getClassLoader())
            .stream()
            .map(ServiceLoader.Provider::get)
            .forEach(installer -> resolvedInstallers.putIfAbsent(platformExtractor.apply(installer), installer));

        this.installersByPlatform = Map.copyOf(resolvedInstallers);
    }

/**
 * Creates a feature installer registry with a module-hint-based dependency message factory.
 *
 * @param installerType       the installer type class
 * @param platformExtractor   function to extract the platform ID from an installer
 * @param moduleHintPrefix    prefix for dependency error hints
 * @param <T>                 the installer type
 * @return the registry
 */
public static <T> @NotNull FeatureInstallerRegistry<T> create(
    @NotNull Class<T> installerType,
    @NotNull Function<? super T, PlatformId> platformExtractor,
    @NotNull String moduleHintPrefix
) {
        Objects.requireNonNull(moduleHintPrefix, "moduleHintPrefix");
        return create(
            installerType,
            platformExtractor,
            platformId -> "Add dependency " + moduleHintPrefix + platformId.name().toLowerCase(Locale.ROOT) + "."
        );
    }

/**
 * Creates a feature installer registry with a custom dependency message factory.
 *
 * @param installerType           the installer type class
 * @param platformExtractor       function to extract the platform ID from an installer
 * @param dependencyHintFactory   function to create dependency error hints per platform
 * @param <T>                     the installer type
 * @return the registry
 */
public static <T> @NotNull FeatureInstallerRegistry<T> create(
    @NotNull Class<T> installerType,
    @NotNull Function<? super T, PlatformId> platformExtractor,
    @NotNull Function<? super PlatformId, String> dependencyHintFactory
) {
        return new FeatureInstallerRegistry<>(installerType, platformExtractor, dependencyHintFactory);
    }

    /**
     * Resolves the installer for the given platform.
     *
     * @param platformId the target platform
     * @return the installer instance
     * @throws IllegalStateException if no installer is found for the platform
     */
    public @NotNull T resolve(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");

        T installer = installersByPlatform.get(platformId);
        if (installer != null) {
            return installer;
        }

        throw new IllegalStateException(missingInstallerMessage(platformId));
    }

    private @NotNull String missingInstallerMessage(@NotNull PlatformId platformId) {
        StringBuilder message = new StringBuilder()
            .append("No ")
            .append(installerType.getSimpleName())
            .append(" found for platform ")
            .append(platformId)
            .append('.');

        String dependencyHint = dependencyHintFactory.apply(platformId);
        if (dependencyHint != null && !dependencyHint.isBlank()) {
            message.append(' ').append(dependencyHint.strip());
        }

        if (installersByPlatform.isEmpty()) {
            message.append(" No installers were discovered via ServiceLoader.");
            return message.toString();
        }

        String availablePlatforms = installersByPlatform.keySet().stream()
            .sorted()
            .map(Enum::name)
            .collect(Collectors.joining(", "));
        message.append(" Available installer platforms: ").append(availablePlatforms).append('.');
        return message.toString();
    }
}
