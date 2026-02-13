package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class FeatureInstallationSupport {
    private FeatureInstallationSupport() {
    }

    public static <T> @NotNull T install(
        @NotNull RapunzelContext context,
        @NotNull Class<T> resultType,
        @Nullable RuntimeCapability capability,
        @NotNull String useCase,
        @NotNull Runnable installAction,
        @NotNull Runnable... dependencies
    ) {
        Objects.requireNonNull(resultType, "resultType");
        return install(
            context,
            current -> current.services().find(resultType).isPresent(),
            () -> context.services().get(resultType),
            capability,
            useCase,
            installAction,
            dependencies
        );
    }

    public static <M> void install(
        @NotNull RapunzelContext context,
        @NotNull Class<M> markerType,
        @NotNull M marker,
        @Nullable RuntimeCapability capability,
        @NotNull String useCase,
        @NotNull Runnable installAction,
        @NotNull Runnable... dependencies
    ) {
        Objects.requireNonNull(markerType, "markerType");
        Objects.requireNonNull(marker, "marker");
        install(
            context,
            current -> current.services().find(markerType).isPresent(),
            () -> marker,
            capability,
            useCase,
            () -> {
                installAction.run();
                context.register(markerType, marker);
            },
            dependencies
        );
    }

    public static <T> @NotNull T install(
        @NotNull RapunzelContext context,
        @NotNull Predicate<? super RapunzelContext> installed,
        @NotNull Supplier<? extends T> installedValue,
        @Nullable RuntimeCapability capability,
        @NotNull String useCase,
        @NotNull Runnable installAction,
        @NotNull Runnable... dependencies
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(installed, "installed");
        Objects.requireNonNull(installedValue, "installedValue");
        Objects.requireNonNull(useCase, "useCase");
        Objects.requireNonNull(installAction, "installAction");
        Objects.requireNonNull(dependencies, "dependencies");

        if (installed.test(context)) {
            return Objects.requireNonNull(installedValue.get(), "installedValue.get()");
        }

        if (capability != null) {
            context.requireCapability(capability, useCase);
        }
        for (Runnable dependency : dependencies) {
            Objects.requireNonNull(dependency, "dependency").run();
        }

        synchronized (context.services()) {
            if (installed.test(context)) {
                return Objects.requireNonNull(installedValue.get(), "installedValue.get()");
            }

            installAction.run();
            return Objects.requireNonNull(installedValue.get(), "installedValue.get()");
        }
    }
}
