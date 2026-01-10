package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;

public record FileSyncResponseChunk(
        @NotNull String requestId,
        @NotNull String groupId,
        int index,
        @NotNull String dataBase64
) {
}

