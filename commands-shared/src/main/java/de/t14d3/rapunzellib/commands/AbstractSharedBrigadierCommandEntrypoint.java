package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for platform-specific Brigadier command entrypoints.
 * <p>
 * Provides a shared {@link #registerSharedCommands(CommandDispatcher)} method
 * that delegates to {@link SharedBrigadierCommandRegistrationSupport} for
 * registering command source stack commands on the given dispatcher.
 */
public abstract class AbstractSharedBrigadierCommandEntrypoint {
    /**
     * Returns the platform identifier associated with this entrypoint.
     *
     * @return the platform ID
     */
    protected abstract @NotNull PlatformId platformId();

    /**
     * Registers shared Brigadier commands on the supplied dispatcher.
     *
     * @param dispatcher the command dispatcher to register commands on
     * @return {@code true} if registration was performed, {@code false} otherwise
     */
    protected final boolean registerSharedCommands(@NotNull CommandDispatcher<?> dispatcher) {
        return SharedBrigadierCommandRegistrationSupport.registerCommandSourceStackCommandsIfAvailable(
            platformId(),
            dispatcher
        );
    }
}
