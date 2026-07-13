package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Static utility for dispatching Minecraft commands.
 * <p>
 * Console dispatch requires a {@link ConsoleCommandDispatcher} service
 * registered in the {@link RapunzelContext}.
 * </p>
 */
public final class RCommands {
    private RCommands() {}

    /**
     * Dispatches a command as the console sender.
     *
     * @param context the RapunzelLib context with a registered ConsoleCommandDispatcher
     * @param command the command to execute (e.g. "/gamemode creative Tester")
     * @throws IllegalStateException if no ConsoleCommandDispatcher is registered
     */
    public static void dispatch(@NotNull RapunzelContext context, @NotNull String command) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(command, "command");
        context.services().get(ConsoleCommandDispatcher.class).dispatch(command);
    }
}
