package de.t14d3.rapunzellib.network.filesync;

import java.util.List;

public record FileSyncResult(
    String groupId,
    int filesWritten,
    int filesDeleted,
    List<String> writtenPaths,
    List<String> deletedPaths
) {
}

