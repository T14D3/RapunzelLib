package de.t14d3.rapunzellib.message;

import org.jetbrains.annotations.NotNull;

public record MessageKey(@NotNull String value) {
    public MessageKey {
        if (value.isBlank()) {
            throw new IllegalArgumentException("value cannot be null/blank");
        }
    }

    public static @NotNull MessageKey of(@NotNull String value) {
        return new MessageKey(value);
    }
}

