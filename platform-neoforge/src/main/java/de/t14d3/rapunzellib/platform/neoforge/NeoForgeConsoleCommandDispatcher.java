package de.t14d3.rapunzellib.platform.neoforge;

import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge implementation of {@link ConsoleCommandDispatcher}.
 * Dispatches commands via the Minecraft server command manager.
 */
public final class NeoForgeConsoleCommandDispatcher implements ConsoleCommandDispatcher {
    private final MinecraftServer server;

    public NeoForgeConsoleCommandDispatcher(@NotNull MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void dispatch(@NotNull String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        CommandSourceStack source = server.createCommandSourceStack();
        server.getCommands().performPrefixedCommand(source, cmd);
    }
}
