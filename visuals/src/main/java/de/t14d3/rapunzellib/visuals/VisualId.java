package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A unique identifier for a visual, backed by a {@link UUID}.
 *
 * @param uuid the underlying UUID
 */
public record VisualId(@NotNull UUID uuid) {

    /**
     * Creates a new visual ID with a randomly generated UUID.
     */
    public VisualId() {
        this(UUID.randomUUID());
    }
}
