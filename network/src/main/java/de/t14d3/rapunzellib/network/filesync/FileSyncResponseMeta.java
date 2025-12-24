package de.t14d3.rapunzellib.network.filesync;

import java.util.List;

public record FileSyncResponseMeta(
    String requestId,
    String groupId,
    boolean ok,
    String error,
    List<String> deletePaths,
    int chunkCount,
    long payloadSize,
    String payloadSha256
) {
}

