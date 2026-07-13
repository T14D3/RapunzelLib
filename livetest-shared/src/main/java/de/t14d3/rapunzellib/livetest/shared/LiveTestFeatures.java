package de.t14d3.rapunzellib.livetest.shared;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.livetest.*;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Static entry point for installing and accessing the live test feature system.
 * <p>
 * Provides methods to install the live test framework into a {@link RapunzelContext},
 * and to access the registered {@link LiveTestHost}, {@link BotService}, and
 * {@link LiveTestRegistry} services.
 * </p>
 * <p>
 * Usage:
 * <pre>{@code
 * // Install into the current context
 * LiveTestFeatures.install();
 *
 * // Get the host for running tests
 * LiveTestHost host = LiveTestFeatures.host();
 *
 * // Register tests via the registry
 * LiveTestFeatures.registry().add(new MyTest());
 * }</pre>
 * </p>
 *
 * @see LiveTestFeatureInstaller
 * @see LiveTestHost
 * @see BotService
 * @see LiveTestRegistry
 */
public final class LiveTestFeatures {

    private static final FeatureInstallerRegistry<LiveTestFeatureInstaller> INSTALLER_REGISTRY =
            FeatureInstallerRegistry.create(
                    LiveTestFeatureInstaller.class,
                    LiveTestFeatureInstaller::platformId,
                    "rapunzellib-livetest-"
            );

    private LiveTestFeatures() {
    }

    /**
     * Installs the live test features using the default Rapunzel context.
     *
     * @return the installed live test host
     * @throws IllegalStateException if no context is available or the platform
     *                               does not support live tests
     */
    public static @NotNull LiveTestHost install() {
        return install(Rapunzel.context());
    }

    /**
     * Installs the live test features into the given context.
     * <p>
     * If already installed, returns the existing {@link LiveTestHost}. This method
     * is idempotent.
     * </p>
     *
     * @param context the Rapunzel context to install into
     * @return the installed live test host
     * @throws IllegalStateException if the platform does not support live tests
     *                               or no {@link LiveTestFeatureInstaller} is found
     */
    public static @NotNull LiveTestHost install(@NotNull RapunzelContext context) {
        Objects.requireNonNull(context, "context");
        return FeatureInstallationSupport.install(
                context,
                LiveTestHost.class,
                RuntimeCapability.LIVETESTS,
                "live test features",
                () -> {
                    // Ensure a registry is available
                    context.getOrCreate(LiveTestRegistry.class, LiveTestRegistry::new);
                    // Resolve and run the platform-specific installer
                    LiveTestFeatureInstaller installer = INSTALLER_REGISTRY.resolve(context.platformId());
                    installer.install(context);
                }
        );
    }

    /**
     * Returns the installed {@link LiveTestHost} from the current context,
     * installing it first if necessary.
     *
     * @return the live test host
     */
    public static @NotNull LiveTestHost host() {
        return install();
    }

    /**
     * Returns the installed {@link LiveTestHost} from the given context,
     * installing it first if necessary.
     *
     * @param context the Rapunzel context
     * @return the live test host
     */
    public static @NotNull LiveTestHost host(@NotNull RapunzelContext context) {
        return install(context);
    }

    /**
     * Returns the {@link BotService} from the current context, if available.
     *
     * @return the bot service, or empty if not installed
     */
    public static @NotNull Optional<BotService> botService() {
        return Rapunzel.findContext()
                .flatMap(ctx -> ctx.services().find(BotService.class));
    }

    /**
     * Returns the {@link BotService} from the given context, if available.
     *
     * @param context the Rapunzel context
     * @return the bot service, or empty if not installed
     */
    public static @NotNull Optional<BotService> botService(@NotNull RapunzelContext context) {
        return context.services().find(BotService.class);
    }

    /**
     * Returns the {@link LiveTestRegistry} from the current context,
     * creating a default one if needed.
     *
     * @return the live test registry
     */
    public static @NotNull LiveTestRegistry registry() {
        return registry(Rapunzel.context());
    }

    /**
     * Returns the {@link LiveTestRegistry} from the given context,
     * creating a default one if needed.
     *
     * @param context the Rapunzel context
     * @return the live test registry
     */
    public static @NotNull LiveTestRegistry registry(@NotNull RapunzelContext context) {
        return context.getOrCreate(LiveTestRegistry.class, LiveTestRegistry::new);
    }

    /**
     * Returns the {@link LiveTestFeatureInstaller} for the given platform, if available.
     *
     * @param platformId the platform identifier
     * @return the installer, or empty if not found
     */
    public static @NotNull Optional<LiveTestFeatureInstaller> installer(@NotNull PlatformId platformId) {
        try {
            return Optional.of(INSTALLER_REGISTRY.resolve(platformId));
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }
}
