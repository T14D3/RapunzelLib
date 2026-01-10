package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RWorldRef(@Nullable String name, @Nullable String key) {
    public RWorldRef {
        boolean hasName = name != null && !name.isBlank();
        boolean hasKey = key != null && !key.isBlank();
        if (!hasName && !hasKey) {
            throw new IllegalArgumentException("Either name or key must be provided");
        }
    }

    public @NotNull String identifier() {
        if (key != null && !key.isBlank()) return key;
        assert name != null;
        return name;
    }
}

