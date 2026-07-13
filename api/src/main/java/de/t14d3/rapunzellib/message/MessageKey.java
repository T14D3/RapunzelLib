package de.t14d3.rapunzellib.message;

import org.jetbrains.annotations.NotNull;

/**
 * A validated message key that ensures the value is non-blank.
 *
 * @param value the message key string
 */
public record MessageKey(@NotNull String value) {
    /**
     * Compact canonical constructor that rejects blank values.
     */
    public MessageKey {
        if (value.isBlank()) {
            throw new IllegalArgumentException("value cannot be null/blank");
        }
    }

    /** Creates a new message key from the given string. */
    public static @NotNull MessageKey of(@NotNull String value) {
        return new MessageKey(value);
    }
}

