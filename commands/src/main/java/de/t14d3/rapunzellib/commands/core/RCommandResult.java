package de.t14d3.rapunzellib.commands.core;

/**
 * Standard result codes for command execution.
 * <p>
 * These codes provide consistent return values for command executors,
 * making it easier to understand command success or failure states.
 * </p>
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * command.executes((source, args) -> {
 *     if (performAction()) {
 *         source.sendMessage("Action completed successfully");
 *         return CommandResult.SUCCESS;
 *     } else {
 *         return CommandResult.FAILURE;
 *     }
 * });
 * }</pre>
 * 
 * @see RCommandExecutor
 * @see RCommandException
 */
public final class RCommandResult {
    
    public static final int SUCCESS = 1;
    
    public static final int FAILURE = 0;
    
    /**
     * The command result should be forwarded to a redirect target.
     * <p>
     * This is typically handled internally by the command framework.
     * </p>
     */
    public static final int FORWARD = 42;
    
    private RCommandResult() {
        // Utility class - prevent instantiation
    }
}
