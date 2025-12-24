package de.t14d3.rapunzellib.message;

public record MessageKey(String value) {
    public MessageKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value cannot be null/blank");
        }
    }

    public static MessageKey of(String value) {
        return new MessageKey(value);
    }
}

