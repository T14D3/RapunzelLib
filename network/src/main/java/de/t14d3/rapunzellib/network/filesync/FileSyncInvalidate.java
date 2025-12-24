package de.t14d3.rapunzellib.network.filesync;

public record FileSyncInvalidate(
    String groupId,
    String invalidateId,
    long createdAt
) {
}

