package de.t14d3.rapunzellib.commands.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown during command execution.
 * <p>
 * This exception represents errors that occur during the execution of a command,
 * such as invalid arguments, permission issues, or business logic errors.
 * </p>
 * <p>
 * Unlike {@link com.mojang.brigadier.exceptions.CommandSyntaxException}, this exception
 * provides a simpler API for command-specific errors and is designed to be caught and
 * handled by the command framework.
 * </p>
 */
public class RCommandException extends Exception {
    private static final String INTERNAL_FAILURE_MESSAGE = "An internal error occurred while running this command.";

    private final int result;
    private final boolean shouldSendToPlayer;
    private final Object @Nullable [] context;
    
    public RCommandException(@NotNull String message) {
        this(message, RCommandResult.FAILURE, true, null, (Object[]) null);
    }
    
    public RCommandException(@NotNull String message, @Nullable Throwable cause) {
        this(message, RCommandResult.FAILURE, true, cause, (Object[]) null);
    }
    
    public RCommandException(
        @NotNull String message,
        boolean shouldSendToPlayer,
        Object @Nullable ... context
    ) {
        this(message, RCommandResult.FAILURE, shouldSendToPlayer, null, context);
    }

    public RCommandException(
        @NotNull String message,
        int result,
        boolean shouldSendToPlayer,
        Object @Nullable ... context
    ) {
        this(message, result, shouldSendToPlayer, null, context);
    }

    public RCommandException(
        @NotNull String message,
        int result,
        boolean shouldSendToPlayer,
        @Nullable Throwable cause,
        Object @Nullable ... context
    ) {
        super(message, cause);
        this.result = result;
        this.shouldSendToPlayer = shouldSendToPlayer;
        this.context = context;
    }

    public int result() {
        return result;
    }
    
    /**
     * Checks if this exception should be shown to the player.
     * <p>
     * Some errors (like internal server errors) should not be shown to players
     * to avoid exposing implementation details.
     * </p>
     * 
     * @return true if this exception should be sent to the player
     */
    public boolean shouldSendToPlayer() {
        return shouldSendToPlayer;
    }
    
    /**
     * Gets the context objects associated with this exception.
     * <p>
     * Context objects can provide additional information for error handling,
     * such as the invalid argument value or related entities.
     * </p>
     * 
     * @return the context objects, or null if none
     */
    public Object @Nullable [] getContext() {
        return context;
    }

    public @NotNull String clientMessage() {
        return shouldSendToPlayer ? getFormattedMessage() : INTERNAL_FAILURE_MESSAGE;
    }
    
    /**
     * Formats the message with the provided context.
     * <p>
     * This method can be used by error handlers to create user-friendly
     * error messages with context information.
     * </p>
     * 
     * @return the formatted message
     */
    @NotNull
    public String getFormattedMessage() {
        String message = getMessage();
        if (message == null || message.isBlank()) {
            message = INTERNAL_FAILURE_MESSAGE;
        }
        if (context == null || context.length == 0) {
            return message;
        }
        
        StringBuilder sb = new StringBuilder(message);
        for (Object ctx : context) {
            if (ctx != null) {
                sb.append(" (context: ").append(ctx).append(")");
            }
        }
        return sb.toString();
    }
}
