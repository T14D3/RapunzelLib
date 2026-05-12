package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Metadata about a file sync response.
 *
 * @param requestId the request this response belongs to
 * @param groupId the sync group identifier
 * @param ok whether the sync was successful
 * @param error error message if not ok
 * @param deletePaths list of paths to delete on the requester
 * @param chunkCount number of data chunks
 * @param payloadSize total payload size in bytes
 * @param payloadSha256 SHA-256 hash of the full payload
 */
public record FileSyncResponseMeta(
        @NotNull String requestId,
        @NotNull String groupId,
        boolean ok,
        @Nullable String error,
        @NotNull List<String> deletePaths,
        int chunkCount,
        long payloadSize,
        @Nullable String payloadSha256
) {
}

