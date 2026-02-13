package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.FeatureInstallationSupport;
import de.t14d3.rapunzellib.context.FeatureInstallerRegistry;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class CommandFeatures {
    private static final FeatureInstallerRegistry<CommandFeatureInstaller> INSTALLER_REGISTRY = FeatureInstallerRegistry.create(
        CommandFeatureInstaller.class,
        CommandFeatureInstaller::platformId,
        "rapunzellib-commands-"
    );

    private CommandFeatures() {
    }

    public static @NotNull RCommandService install() {
        return install(Rapunzel.context());
    }

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

    public static @NotNull CommandSourceAdapters adapters() {
        install();
        return Rapunzel.context().services().get(CommandSourceAdapters.class);
    }

    public static @NotNull RCommandService commands() {
        return install();
    }

    public static @NotNull Optional<SharedBrigadierCommandRegistrar<?>> brigadierRegistrar() {
        @SuppressWarnings("rawtypes")
        Optional<SharedBrigadierCommandRegistrar> registrar = Rapunzel.context().services().find(SharedBrigadierCommandRegistrar.class);
        return registrar.map(value -> (SharedBrigadierCommandRegistrar<?>) value);
    }
}
