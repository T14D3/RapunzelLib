package de.t14d3.rapunzellib.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

/**
 * Registers shared Rapunzel commands into a Brigadier {@link CommandDispatcher}.
 * <p>
 * Implementations attach all registered commands from the command service
 * to a platform-specific Brigadier dispatcher, enabling cross-platform
 * command registration.
 * </p>
 *
 * @param <S> the Brigadier command source type
 */
public interface SharedBrigadierCommandRegistrar<S> {
    /**
     * Gets the platform identifier.
     *
     * @return the platform ID
     */
    @NotNull PlatformId platformId();

    /**
     * Gets the Brigadier source type.
     *
     * @return the source type class
     */
    @NotNull Class<S> sourceType();

    /**
     * Registers all shared commands into the given Brigadier dispatcher.
     *
     * @param dispatcher the Brigadier command dispatcher
     */
    void registerSharedCommands(@NotNull CommandDispatcher<S> dispatcher);
}
