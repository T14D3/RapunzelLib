package de.t14d3.rapunzellib.platform.sponge;

import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.exception.CommandException;

/**
 * Sponge implementation of {@link ConsoleCommandDispatcher}.
 * Dispatches commands via the Sponge command manager.
 */
public final class SpongeConsoleCommandDispatcher implements ConsoleCommandDispatcher {
    private final Server server;
    private final SystemSubject consoleSubject;

    public SpongeConsoleCommandDispatcher(@NotNull Server server) {
        this.server = server;
        this.consoleSubject = server.game().systemSubject();
    }

    @Override
    public void dispatch(@NotNull String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        try {
            server.commandManager().process(consoleSubject, cmd);
        } catch (CommandException e) {
            throw new RuntimeException("Failed to dispatch command: " + command, e);
        }
    }
}
