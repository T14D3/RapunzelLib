package de.t14d3.rapunzellib.platform.fabric;

import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

/**
 * Fabric implementation of {@link ConsoleCommandDispatcher}.
 * Dispatches commands via the Minecraft server command manager.
 */
public final class FabricConsoleCommandDispatcher implements ConsoleCommandDispatcher {
    private final MinecraftServer server;

    public FabricConsoleCommandDispatcher(@NotNull MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void dispatch(@NotNull String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        CommandSourceStack source = server.createCommandSourceStack();
        server.getCommands().performPrefixedCommand(source, cmd);
    }
}
