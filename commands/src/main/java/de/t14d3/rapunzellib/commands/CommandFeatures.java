package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Entry point for installing and accessing the command feature system.
 * <p>
 * Provides static methods to install the command framework into the current
 * {@link de.t14d3.rapunzellib.context.RapunzelContext}, and to retrieve the
 * registered {@link RCommandService}, {@link CommandSourceAdapters}, and
 * optional {@link SharedBrigadierCommandRegistrar}.
 * </p>
 */
public final class CommandFeatures {
    /**
     * The installer registry for resolving platform-specific command feature installers.
     */
    private static final FeatureInstallerRegistry<CommandFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        CommandFeatureInstaller.class,
        CommandFeatureInstaller::platformId,
        "rapunzellib-commands-"
    );

    private CommandFeatures() {
    }

    /**
     * Installs the command features using the default Rapunzel context.
     *
     * @return the installed command service
     */
    public static @NotNull RCommandService install() {
        return install(Rapunzel.context());
    }

    /**
     * Installs the command features into the given context.
     *
     * @param context the Rapunzel context
     * @return the installed command service
     */
    public static @NotNull RCommandService install(@NotNull RapunzelContext context) {
        return FeatureInstallationSupport.install(
            context,
            current -> current.services().find(CommandSourceAdapters.class).isPresent()
                && current.services().find(RCommandService.class).isPresent(),
            () -> context.services().get(RCommandService.class),
            RuntimeCapability.COMMANDS,
            "command features",
            () -> INSTALLER_REGISTRY.resolve(context.platformId()).install(context)
        );
    }

    /**
     * Gets (or installs) the command source adapters.
     *
     * @return the command source adapters
     */
    public static @NotNull CommandSourceAdapters adapters() {
        install();
        return Rapunzel.context().services().get(CommandSourceAdapters.class);
    }

    /**
     * Gets (or installs) the command service.
     *
     * @return the command service
     */
    public static @NotNull RCommandService commands() {
        return install();
    }

    /**
     * Gets the optional shared Brigadier command registrar.
     *
     * @return the registrar, or empty if not available
     */
    public static @NotNull Optional<SharedBrigadierCommandRegistrar<?>> brigadierRegistrar() {
        @SuppressWarnings("rawtypes")
        Optional<SharedBrigadierCommandRegistrar> registrar = Rapunzel.context().services().find(SharedBrigadierCommandRegistrar.class);
        return registrar.map(value -> (SharedBrigadierCommandRegistrar<?>) value);
    }
}
