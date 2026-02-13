package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSharedBrigadierCommandEntrypoint {
    protected abstract @NotNull PlatformId platformId();

    protected final boolean registerSharedCommands(@NotNull CommandDispatcher<?> dispatcher) {
        return SharedBrigadierCommandRegistrationSupport.registerCommandSourceStackCommandsIfAvailable(
            platformId(),
            dispatcher
        );
    }
}
