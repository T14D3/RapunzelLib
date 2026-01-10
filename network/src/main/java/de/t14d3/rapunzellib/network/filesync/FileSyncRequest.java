package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record FileSyncRequest(
        @NotNull String requestId,
        @NotNull String groupId,
        @NotNull Map<String, String> fileHashes
) {
}

