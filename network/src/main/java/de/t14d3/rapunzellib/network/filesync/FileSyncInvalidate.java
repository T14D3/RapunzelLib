package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;

/**
 * A network message signalling that cached files in a group should be invalidated.
 *
 * @param groupId the sync group identifier
 * @param invalidateId unique identifier for this invalidation
 * @param createdAt timestamp when this invalidation was created
 */
public record FileSyncInvalidate(
        @NotNull String groupId,
        @NotNull String invalidateId,
        long createdAt
) {
}

