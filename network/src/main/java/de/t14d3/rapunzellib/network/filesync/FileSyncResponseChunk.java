package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;

/**
 * A chunk of file data sent as part of a file sync response.
 *
 * @param requestId the request this chunk belongs to
 * @param groupId the sync group identifier
 * @param index the chunk index (0-based)
 * @param dataBase64 base64-encoded chunk data
 */
public record FileSyncResponseChunk(
        @NotNull String requestId,
        @NotNull String groupId,
        int index,
        @NotNull String dataBase64
) {
}

