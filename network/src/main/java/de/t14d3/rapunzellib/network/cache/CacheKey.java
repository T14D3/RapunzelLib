package de.t14d3.rapunzellib.network.cache;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a cache key for identifying cached entities.
 */
public record CacheKey(@NotNull String entityClassName, @NotNull String id) {
    
    public CacheKey {
        if (entityClassName == null || entityClassName.isBlank()) {
            throw new IllegalArgumentException("entityClassName cannot be null or blank");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or blank");
        }
    }
}
