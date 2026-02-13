package de.t14d3.rapunzellib.network.cache;

import org.jetbrains.annotations.NotNull;

/**
 * Message representing a cache invalidation event.
 */
public record CacheInvalidationMessage(
    @NotNull String entityClassName,
    @NotNull String id,
    @NotNull InvalidationOperation operation,
    @NotNull String serverName,
    long timestamp
) {
    
    public CacheInvalidationMessage {
        if (entityClassName == null || entityClassName.isBlank()) {
            throw new IllegalArgumentException("entityClassName cannot be null or blank");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or blank");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation cannot be null");
        }
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("serverName cannot be null or blank");
        }
    }
}
