package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Entry point for installing and accessing the RapunzelLib game event system.
 *
 * <p>Provides static factory methods to obtain the {@link GameEventBus}, query platform
 * {@link GameEventSupportManifest support manifests}, and install platform-specific
 * event bridges via {@link GameEventBridgeInstaller}.</p>
 */
public final class GameEvents {
    private static final FeatureInstallerRegistry<GameEventBridgeInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        GameEventBridgeInstaller.class,
        GameEventBridgeInstaller::platformId,
        "rapunzellib-events-"
    );

    private GameEvents() {
    }

    /**
     * Returns the installed {@link GameEventBus}, installing it if necessary.
     *
     * @return the global game event bus
     */
    public static @NotNull GameEventBus bus() {
        return install();
    }

    /**
     * Returns the {@link GameEventSupportManifest} for the current platform.
     *
     * @return the support manifest
     */
    public static @NotNull GameEventSupportManifest support() {
        RapunzelContext context = Rapunzel.context();
        return context.services().find(GameEventSupportManifest.class).orElseGet(() -> support(context.runtime()));
    }

    /**
     * Returns the {@link GameEventSupportManifest} for the given runtime.
     *
     * @param runtime the platform runtime
     * @return the support manifest
     */
    public static @NotNull GameEventSupportManifest support(@NotNull PlatformRuntime runtime) {
        Objects.requireNonNull(runtime, "runtime");
        if (!runtime.hasCapability(RuntimeCapability.EVENTS)) {
            return GameEventSupportManifest.empty(runtime.platformId());
        }
        return support(runtime.platformId());
    }

    /**
     * Returns the {@link GameEventSupportManifest} for the given platform ID.
     *
     * @param platformId the platform identifier
     * @return the support manifest
     */
    public static @NotNull GameEventSupportManifest support(@NotNull PlatformId platformId) {
        Objects.requireNonNull(platformId, "platformId");
        GameEventSupportManifest manifest = findInstaller(platformId)
            .map(GameEventBridgeInstaller::supportManifest)
            .orElseGet(() -> GameEventSupportManifest.empty(platformId));
        for (GameEventSupportContributor contributor : ServiceLoader.load(GameEventSupportContributor.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .filter(contributor -> contributor.platformId() == platformId)
            .sorted((left, right) -> left.getClass().getName().compareTo(right.getClass().getName()))
            .toList()) {
            manifest = manifest.overlayUnsupported(contributor.supportManifest());
        }
        return manifest;
    }

    /**
     * Installs the game event system using the current {@link RapunzelContext}.
     *
     * @return the installed event bus
     */
    public static @NotNull GameEventBus install() {
        return install(Rapunzel.context());
    }

    /**
     * Installs the game event system using the given context.
     *
     * @param ctx the Rapunzel context
     * @return the installed event bus
     */
    public static @NotNull GameEventBus install(@NotNull RapunzelContext ctx) {
        return FeatureInstallationSupport.install(
            ctx,
            current -> current.services().find(GameEventBridge.class).isPresent(),
            () -> {
                ctx.getOrCreate(GameEventSupportManifest.class, () -> support(ctx.runtime()));
                return ctx.services().get(GameEventBus.class);
            },
            RuntimeCapability.EVENTS,
            "game events",
            () -> {
                GameEventBus bus = ctx.getOrCreate(GameEventBus.class, () -> new GameEventBus(ctx.scheduler(), ctx.logger()));
                GameEventBridgeInstaller installer = INSTALLER_REGISTRY.resolve(ctx.platformId());
                GameEventSupportManifest manifest = support(ctx.runtime());
                GameEventBridge bridge = installer.install(ctx, bus);
                ctx.register(GameEventSupportManifest.class, manifest);
                ctx.register(GameEventBridge.class, bridge);
            }
        );
    }

    /**
     * Finds the {@link GameEventBridgeInstaller} for the given platform ID.
     *
     * @param platformId the platform identifier
     * @return an optional containing the installer, or empty if none is registered
     */
    private static @NotNull Optional<GameEventBridgeInstaller> findInstaller(@NotNull PlatformId platformId) {
        try {
            return Optional.of(INSTALLER_REGISTRY.resolve(platformId));
        } catch (IllegalStateException ignored) {
            return Optional.empty();
        }
    }
}
