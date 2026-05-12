package de.t14d3.rapunzellib.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An immutable reference to a world, identified by name and/or key.
 *
 * <p>At least one of {@code name} or {@code key} must be non-null and non-blank.</p>
 *
 * @param name the world name, may be null if key is provided
 * @param key  the world key, may be null if name is provided
 */
public record RWorldRef(@Nullable String name, @Nullable RKey key) {
    public RWorldRef {
        boolean hasName = name != null && !name.isBlank();
        boolean hasKey = key != null;
        if (!hasName && !hasKey) {
            throw new IllegalArgumentException("Either name or key must be provided");
        }
    }

    /**
     * Creates a world reference from an optional name and a string key.
     *
     * @param name the world name, may be null
     * @param key  the world key string, may be null or blank
     */
    public RWorldRef(@Nullable String name, @Nullable String key) {
        this(name, key == null || key.isBlank() ? null : RKey.of(key));
    }

    /**
     * Creates a world reference from a key.
     *
     * @param key the world key
     */
    public RWorldRef(@NotNull RKey key) {
        this(null, key);
    }

    /**
     * Returns a human-readable identifier for this world reference.
     *
     * @return the key string if available, otherwise the name
     */
    public @NotNull String identifier() {
        if (key != null) return key.asString();
        assert name != null;
        return name;
    }
}
