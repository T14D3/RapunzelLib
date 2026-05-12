package de.t14d3.rapunzellib.network.cache;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a cache key for identifying cached entities.
 *
 * @param entityClassName the fully qualified class name of the entity
 * @param id              the entity identifier
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
