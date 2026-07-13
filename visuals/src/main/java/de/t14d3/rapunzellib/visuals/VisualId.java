package de.t14d3.rapunzellib.visuals;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record VisualId(@NotNull UUID uuid) {

    public VisualId() {
        this(UUID.randomUUID());
    }
}
