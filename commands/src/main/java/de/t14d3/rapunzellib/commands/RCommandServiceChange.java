package de.t14d3.rapunzellib.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record RCommandServiceChange(
    @NotNull Type type,
    @Nullable RegisteredCommandTree registration,
    boolean queued
) {
    public RCommandServiceChange {
        Objects.requireNonNull(type, "type");
        if (type != Type.FLUSH_REQUESTED) {
            Objects.requireNonNull(registration, "registration");
        }
    }

    public enum Type {
        REGISTERED,
        UNREGISTERED,
        FLUSH_REQUESTED
    }
}
