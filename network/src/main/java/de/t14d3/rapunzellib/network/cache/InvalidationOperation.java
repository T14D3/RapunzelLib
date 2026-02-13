package de.t14d3.rapunzellib.network.cache;

/**
 * Enum representing cache invalidation operations.
 */
public enum InvalidationOperation {
    /**
     * Entity was updated or inserted.
     */
    UPSERT,
    
    /**
     * Entity was deleted.
     */
    DELETE
}
