package de.t14d3.rapunzellib.commands;

import org.jetbrains.annotations.NotNull;

/**
 * Something that can execute a Minecraft command.
 * Implementations include players, console, and command blocks.
 */
@FunctionalInterface
public interface RCommandDispatcher {
    /**
     * Executes a command as this dispatcher.
     *
     * @param command the command to execute (may include or omit the leading "/")
     */
    void dispatch(@NotNull String command);
}
