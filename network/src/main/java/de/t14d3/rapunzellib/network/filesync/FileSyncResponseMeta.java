package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

