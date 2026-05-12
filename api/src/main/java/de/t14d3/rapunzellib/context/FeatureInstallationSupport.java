package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility for safely installing platform features into a {@link RapunzelContext}.
 *
 * <p>Handles idempotency checks, capability requirements, dependency ordering,
 * and synchronization.</p>
 */
public final class FeatureInstallationSupport {
    private FeatureInstallationSupport() {
    }

    /**
     * Installs a feature and returns a service instance of the given type.
     *
     * @param context       the context to install into
     * @param resultType    the service type to return
     * @param capability    the required capability, may be null
     * @param useCase       the use case description for error messages
     * @param installAction the action to perform the installation
     * @param dependencies  actions for dependent features
     * @param <T>           the result type
     * @return the installed service instance
     */
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

    /**
     * Installs a feature and registers a marker instance to track installation state.
     *
     * @param context       the context to install into
     * @param markerType    the marker type class
     * @param marker        the marker instance
     * @param capability    the required capability, may be null
     * @param useCase       the use case description
     * @param installAction the action to perform the installation
     * @param dependencies  actions for dependent features
     * @param <M>           the marker type
     */
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

/**
 * Core installation method with full control over state checking.
 *
 * @param context        the context to install into
 * @param installed      predicate to check if already installed
 * @param installedValue supplier for the installed value
 * @param capability     the required capability, may be null
 * @param useCase        the use case description
 * @param installAction  the action to perform the installation
 * @param dependencies   actions for dependent features
 * @param <T>            the result type
 * @return the installed value
 */
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
