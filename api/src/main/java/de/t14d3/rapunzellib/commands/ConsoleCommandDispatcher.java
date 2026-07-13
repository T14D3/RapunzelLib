package de.t14d3.rapunzellib.commands;

import org.jetbrains.annotations.NotNull;

/**
 * Console command dispatcher marker interface.
 * Registered as a service in {@link de.t14d3.rapunzellib.context.RapunzelContext}
 * so static utilities can dispatch commands via the console.
 */
@FunctionalInterface
public interface ConsoleCommandDispatcher extends RCommandDispatcher {
}
