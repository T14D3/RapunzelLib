package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when NBT serialization or deserialization fails.
 * <p>
 * This is a runtime exception used by {@link SerializedEntity}, {@link SerializedBlockEntity},
 * and related serialization utilities.</p>
 */
public class SerializationException extends RuntimeException {
    
    /**
     * Constructs a new SerializationException with the specified message.
     *
     * @param message the detail message
     */
    public SerializationException(@NotNull String message) {
        super(message);
    }
    
    /**
     * Constructs a new SerializationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public SerializationException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new SerializationException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public SerializationException(@Nullable Throwable cause) {
        super(cause);
    }
}
