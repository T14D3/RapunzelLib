package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RWorldRef(@Nullable String name, @Nullable RKey key) {
    public RWorldRef {
        boolean hasName = name != null && !name.isBlank();
        boolean hasKey = key != null;
        if (!hasName && !hasKey) {
            throw new IllegalArgumentException("Either name or key must be provided");
        }
    }

    public RWorldRef(@Nullable String name, @Nullable String key) {
        this(name, key == null || key.isBlank() ? null : RKey.of(key));
    }

    public RWorldRef(@NotNull RKey key) {
        this(null, key);
    }

    public @NotNull String identifier() {
        if (key != null) return key.asString();
        assert name != null;
        return name;
    }
}
