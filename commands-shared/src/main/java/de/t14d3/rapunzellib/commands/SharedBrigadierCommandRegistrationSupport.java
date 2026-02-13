package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class SharedBrigadierCommandRegistrationSupport {
    private static final String MOJANG_COMMAND_SOURCE_STACK_CLASS_NAME = "net.minecraft.commands.CommandSourceStack";

    private SharedBrigadierCommandRegistrationSupport() {
    }

    public static boolean registerCommandSourceStackCommandsIfAvailable(
        @NotNull PlatformId platformId,
        @NotNull CommandDispatcher<?> dispatcher
    ) {
        return register(platformId, commandSourceStackType(), dispatcher);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean register(
        @NotNull PlatformId platformId,
        @NotNull Class<?> sourceType,
        @NotNull CommandDispatcher<?> dispatcher
    ) {
        if (!Rapunzel.isBootstrapped()) {
            return false;
        }

        RapunzelContext context = Rapunzel.context();
        if (context.platformId() != platformId || !context.supports(RuntimeCapability.COMMANDS)) {
            return false;
        }

        CommandFeatures.install();

        Optional<SharedBrigadierCommandRegistrar<?>> registrar = CommandFeatures.brigadierRegistrar();
        if (registrar.isEmpty()) {
            return false;
        }

        SharedBrigadierCommandRegistrar<?> sharedRegistrar = registrar.get();
        if (sharedRegistrar.platformId() != platformId || sharedRegistrar.sourceType() != sourceType) {
            return false;
        }

        Runnable registration = () -> ((SharedBrigadierCommandRegistrar) sharedRegistrar).registerSharedCommands(dispatcher);
        context.services()
            .find(SharedRuntimeCommandRegistrationSupport.class)
            .ifPresentOrElse(
                support -> support.sync(dispatcher, registration),
                registration
            );
        return true;
    }

    private static @NotNull Class<?> commandSourceStackType() {
        try {
            return Class.forName(MOJANG_COMMAND_SOURCE_STACK_CLASS_NAME);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                "Missing Shared command source stack type: " + MOJANG_COMMAND_SOURCE_STACK_CLASS_NAME,
                ex
            );
        }
    }
}
