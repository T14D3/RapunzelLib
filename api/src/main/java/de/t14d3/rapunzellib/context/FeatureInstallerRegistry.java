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

    public static <T> @NotNull FeatureInstallerRegistry<T> create(
        @NotNull Class<T> installerType,
        @NotNull Function<? super T, PlatformId> platformExtractor,
        @NotNull Function<? super PlatformId, String> dependencyHintFactory
    ) {
        return new FeatureInstallerRegistry<>(installerType, platformExtractor, dependencyHintFactory);
    }

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
