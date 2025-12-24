package de.t14d3.rapunzellib.network.filesync;

import java.util.Map;

public record FileSyncRequest(
    String requestId,
    String groupId,
    Map<String, String> fileHashes
) {
}

