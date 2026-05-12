package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A network message requesting file synchronization for a group.
 *
 * @param requestId unique identifier for this request
 * @param groupId the sync group identifier
 * @param fileHashes map of file paths to their SHA-256 hashes on the requester
 */
public record FileSyncRequest(
        @NotNull String requestId,
        @NotNull String groupId,
        @NotNull Map<String, String> fileHashes
) {
}

