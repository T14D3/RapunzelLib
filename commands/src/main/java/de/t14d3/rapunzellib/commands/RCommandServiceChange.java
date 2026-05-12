package de.t14d3.rapunzellib.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a change to the command service.
 * <p>
 * Records the type of change, the affected registration (if any),
 * and whether the change is queued or immediate.
 * </p>
 *
 * @param type         the type of change
 * @param registration the affected registered command tree, or null for flush events
 * @param queued       whether the change is queued
 */
public record RCommandServiceChange(
    @NotNull Type type,
    @Nullable RegisteredCommandTree registration,
    boolean queued
) {
    /**
     * Validates the record components.
     *
     * @throws NullPointerException if type is null, or if registration is null for non-flush types
     */
    public RCommandServiceChange {
        Objects.requireNonNull(type, "type");
        if (type != Type.FLUSH_REQUESTED) {
            Objects.requireNonNull(registration, "registration");
        }
    }

    /**
     * The type of command service change.
     */
    public enum Type {
        /** A command tree was registered. */
        REGISTERED,
        /** A command tree was unregistered. */
        UNREGISTERED,
        /** A flush was requested. */
        FLUSH_REQUESTED
    }
}
