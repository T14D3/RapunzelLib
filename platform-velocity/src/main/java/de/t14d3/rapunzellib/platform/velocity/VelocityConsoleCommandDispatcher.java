package de.t14d3.rapunzellib.platform.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import org.jetbrains.annotations.NotNull;

/**
 * Velocity implementation of {@link ConsoleCommandDispatcher}.
 * Dispatches commands via the Velocity command manager.
 */
public final class VelocityConsoleCommandDispatcher implements ConsoleCommandDispatcher {
    private final ProxyServer proxy;

    public VelocityConsoleCommandDispatcher(@NotNull ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void dispatch(@NotNull String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        CommandManager commandManager = proxy.getCommandManager();
        CommandSource consoleSource = proxy.getConsoleCommandSource();
        commandManager.executeAsync(consoleSource, cmd);
    }
}
