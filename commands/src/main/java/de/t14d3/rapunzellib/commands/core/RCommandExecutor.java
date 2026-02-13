package de.t14d3.rapunzellib.commands.core;

import de.t14d3.rapunzellib.commands.RCommandSource;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for executing commands.
 * <p>
 * Command executors are called when a command node is reached during command processing.
 * They contain the actual implementation logic for what the command should do.
 * </p>
 * <p>
 * Implementations of this interface are typically implemented as lambdas or method references
 * for concise command definitions:
 * </p>
 * <pre>{@code
 * command.executes((source, args) -> {
 *     source.sendMessage("Hello, world!");
 *     return RCommandResult.SUCCESS;
 * });
 * }</pre>
 * 
 * @param <S> the command source type, typically {@link RCommandSource}
 * @see RCommandNode#executes(RCommandExecutor) 
 */
@FunctionalInterface
public interface RCommandExecutor<S extends RCommandSource> {
    
    /**
     * Executes this command with the given source and arguments.
     * 
     * @param source the command source executing this command
     * @param args the command arguments
     * @return the result of command execution
     * @throws RCommandException if an error occurs during execution
     */
    int execute(@NotNull S source, @NotNull RCommandArguments<S> args) throws RCommandException;
}
