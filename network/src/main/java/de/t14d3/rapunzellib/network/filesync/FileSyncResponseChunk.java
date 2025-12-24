package de.t14d3.rapunzellib.network.filesync;

public record FileSyncResponseChunk(
    String requestId,
    String groupId,
    int index,
    String dataBase64
) {
}

