package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record FileSyncResult(
        @NotNull String groupId,
        int filesWritten,
        int filesDeleted,
        @NotNull List<String> writtenPaths,
        @NotNull List<String> deletedPaths
) {
}

