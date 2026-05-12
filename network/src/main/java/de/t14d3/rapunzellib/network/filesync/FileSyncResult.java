package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The result of a file synchronization operation.
 *
 * @param groupId the sync group that was processed
 * @param filesWritten number of files written
 * @param filesDeleted number of files deleted
 * @param writtenPaths list of paths that were written
 * @param deletedPaths list of paths that were deleted
 */
public record FileSyncResult(
        @NotNull String groupId,
        int filesWritten,
        int filesDeleted,
        @NotNull List<String> writtenPaths,
        @NotNull List<String> deletedPaths
) {
}

