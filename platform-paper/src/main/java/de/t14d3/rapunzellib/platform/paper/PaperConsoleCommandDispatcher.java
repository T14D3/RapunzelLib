package de.t14d3.rapunzellib.platform.paper;

import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Paper implementation of {@link ConsoleCommandDispatcher}.
 * Dispatches commands via {@code Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ...)}.
 */
public final class PaperConsoleCommandDispatcher implements ConsoleCommandDispatcher {
    @Override
    public void dispatch(@NotNull String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
