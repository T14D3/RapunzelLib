package de.t14d3.rapunzellib.network.filesync;

import org.jetbrains.annotations.NotNull;

public record FileSyncInvalidate(
        @NotNull String groupId,
        @NotNull String invalidateId,
        long createdAt
) {
}

