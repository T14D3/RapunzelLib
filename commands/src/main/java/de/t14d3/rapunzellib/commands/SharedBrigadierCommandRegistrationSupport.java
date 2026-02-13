package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class SharedBrigadierCommandRegistrationSupport {
    private SharedBrigadierCommandRegistrationSupport() {
    }

    public static <S> boolean registerSharedCommandsIfAvailable(
        @NotNull PlatformId platformId,
        @NotNull Class<S> sourceType,
        @NotNull CommandDispatcher<S> dispatcher
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(dispatcher, "dispatcher");

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

        register(dispatcher, sharedRegistrar);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <S> void register(
        @NotNull CommandDispatcher<S> dispatcher,
        @NotNull SharedBrigadierCommandRegistrar<?> registrar
    ) {
        ((SharedBrigadierCommandRegistrar<S>) registrar).registerSharedCommands(dispatcher);
    }
}
